package kr.pincoin.api.domain.coordinator.user

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.response.AccessTokenResponse
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.domain.user.service.UserService
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakCreateUserRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLoginRequest
import kr.pincoin.api.external.auth.keycloak.api.response.*
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import kr.pincoin.api.external.auth.keycloak.service.KeycloakApiClient
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.global.security.error.AuthErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserResourceCoordinator(
    private val userService: UserService,
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakProperties: KeycloakProperties
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Keycloak과 DB에 동시에 사용자 생성
     * 보상 트랜잭션으로 일관성 보장
     */
    @Transactional
    suspend fun createUserWithKeycloak(
        request: SignUpRequest,
        adminToken: String
    ): User = withContext(Dispatchers.IO) {
        logger.info { "사용자 생성 시작: email=${request.email}" }

        var keycloakUserId: String? = null

        try {
            // 1. Keycloak에 사용자 생성
            keycloakUserId = createKeycloakUser(request, adminToken)
            logger.info { "Keycloak 사용자 생성 완료: userId=$keycloakUserId" }

            // 2. DB에 사용자 생성 (Keycloak ID 연결)
            val user = userService.createUser(request, keycloakUserId)
            logger.info { "DB 사용자 생성 완료: keycloakId=$keycloakUserId, dbUserId=${user.id}" }

            user

        } catch (e: BusinessException) {
            logger.error { "사용자 생성 비즈니스 오류: email=${request.email}, error=$e" }

            // DB 생성 실패시 Keycloak 사용자 삭제 (보상 트랜잭션)
            keycloakUserId?.let { userId ->
                executeCompensatingTransaction(userId, adminToken, request.email)
            }

            throw e
        } catch (e: Exception) {
            logger.error { "사용자 생성 시스템 오류: email=${request.email}, error=$e" }

            // DB 생성 실패시 Keycloak 사용자 삭제 (보상 트랜잭션)
            keycloakUserId?.let { userId ->
                executeCompensatingTransaction(userId, adminToken, request.email)
            }

            throw BusinessException(UserErrorCode.SYSTEM_ERROR)
        }
    }

    /**
     * 사용자 인증 및 토큰 발급
     */
    suspend fun authenticateUser(email: String, password: String): AccessTokenResponse = withContext(Dispatchers.IO) {
        logger.info { "사용자 인증 시작: email=$email" }

        val loginRequest = KeycloakLoginRequest(
            clientId = keycloakProperties.clientId,
            clientSecret = keycloakProperties.clientSecret,
            username = email,
            password = password,
            grantType = "password",
            scope = "openid profile email"
        )

        return@withContext when (val response = keycloakApiClient.login(loginRequest)) {
            is KeycloakResponse.Success -> {
                logger.info { "사용자 인증 성공: email=$email" }
                val tokenData = response.data
                AccessTokenResponse.of(
                    accessToken = tokenData.accessToken,
                    expiresIn = tokenData.expiresIn
                )
            }

            is KeycloakResponse.Error -> {
                logger.error { "사용자 인증 실패: email=$email, error=${response.errorCode}" }

                val errorCode = when (response.errorCode) {
                    "invalid_grant" -> AuthErrorCode.INVALID_CREDENTIALS
                    "invalid_client" -> AuthErrorCode.INVALID_CREDENTIALS
                    "TIMEOUT" -> AuthErrorCode.LOGIN_TIMEOUT
                    else -> AuthErrorCode.AUTHENTICATION_FAILED
                }

                throw BusinessException(errorCode)
            }
        }
    }

    /**
     * Keycloak에 사용자 생성
     */
    private suspend fun createKeycloakUser(
        request: SignUpRequest,
        adminToken: String
    ): String {
        logger.info { "Keycloak 사용자 생성 시작: email=${request.email}" }

        val createUserRequest = KeycloakCreateUserRequest(
            username = request.email,
            email = request.email,
            firstName = request.name, // 한국 시스템: 전체 이름을 firstName에 저장
            lastName = "", // 빈 문자열로 설정 (400 에러 방지)
            enabled = true,
            emailVerified = false,
            credentials = listOf(
                KeycloakCreateUserRequest.KeycloakCredential(
                    type = "password",
                    value = request.password,
                    temporary = false
                )
            )
        )

        return when (val response = keycloakApiClient.createUser(adminToken, createUserRequest)) {
            is KeycloakResponse.Success -> {
                logger.info { "Keycloak 사용자 생성 성공: userId=${response.data.userId}, email=${request.email}" }
                response.data.userId
            }

            is KeycloakResponse.Error -> {
                logger.error { "Keycloak 사용자 생성 실패: email=${request.email}, error=${response.errorCode}" }

                val errorCode = when (response.errorCode) {
                    "TIMEOUT" -> AuthErrorCode.KEYCLOAK_TIMEOUT
                    "SYSTEM_ERROR" -> AuthErrorCode.KEYCLOAK_SYSTEM_ERROR
                    else -> UserErrorCode.USER_CREATE_FAILED
                }

                throw BusinessException(errorCode)
            }
        }
    }

    /**
     * 보상 트랜잭션: Keycloak에서 사용자 삭제
     * DB 생성 실패시 Keycloak에 생성된 사용자를 정리
     */
    private suspend fun executeCompensatingTransaction(
        keycloakUserId: String,
        adminToken: String,
        email: String
    ) {
        logger.warn { "보상 트랜잭션 시작: Keycloak 사용자 삭제 - userId=$keycloakUserId, email=$email" }

        try {
            when (val response = keycloakApiClient.deleteUser(adminToken, keycloakUserId)) {
                is KeycloakResponse.Success -> {
                    logger.info { "보상 트랜잭션 성공: Keycloak 사용자 삭제 완료 - userId=$keycloakUserId" }
                }

                is KeycloakResponse.Error -> {
                    logger.error {
                        "보상 트랜잭션 실패: Keycloak 사용자 삭제 오류 - userId=$keycloakUserId, error=${response.errorCode}"
                    }
                    // 보상 트랜잭션 실패는 별도 알림/모니터링이 필요할 수 있음
                    notifyCompensationFailure(keycloakUserId, email, response.errorCode)
                }
            }
        } catch (e: Exception) {
            logger.error {
                "보상 트랜잭션 예외 발생: userId=$keycloakUserId, email=$email, error=${e.message}"
            }
            notifyCompensationFailure(keycloakUserId, email, e.message ?: "UNKNOWN_ERROR")
        }
    }

    /**
     * 보상 트랜잭션 실패 알림
     * 실제 운영환경에서는 모니터링 시스템이나 알림 시스템과 연동
     */
    private fun notifyCompensationFailure(keycloakUserId: String, email: String, errorMessage: String) {
        logger.error {
            "긴급: 보상 트랜잭션 실패로 인한 데이터 불일치 발생 - " +
                    "keycloakUserId=$keycloakUserId, email=$email, error=$errorMessage"
        }

        // TODO: 실제 구현시 다음과 같은 처리 필요
        // 1. 모니터링 시스템 알림 (예: Sentry, Datadog 등)
        // 2. 관리자 이메일/슬랙 알림
        // 3. 수동 정리가 필요한 데이터 목록에 추가
        // 4. 배치 작업으로 주기적 정리 대상에 추가
    }
}