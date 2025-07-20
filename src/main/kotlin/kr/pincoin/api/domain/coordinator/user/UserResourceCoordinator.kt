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
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakCreateUserResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakDeleteUserResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakErrorResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakTokenResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import kr.pincoin.api.external.auth.keycloak.service.KeycloakApiClient
import kr.pincoin.api.global.exception.BusinessException
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
            is KeycloakTokenResponse -> {
                logger.info { "사용자 인증 성공: email=$email" }
                AccessTokenResponse.of(
                    accessToken = response.accessToken,
                    expiresIn = response.expiresIn
                )
            }

            is KeycloakErrorResponse -> {
                logger.error { "사용자 인증 실패: email=$email, error=${response.error}" }
                throw BusinessException(UserErrorCode.AUTHENTICATION_FAILED)
            }

            else -> {
                logger.error { "사용자 인증 응답 파싱 실패: email=$email" }
                throw BusinessException(UserErrorCode.AUTHENTICATION_PARSING_ERROR)
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
            firstName = request.name,
            lastName = "",
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
            is KeycloakCreateUserResponse -> {
                logger.info { "Keycloak 사용자 생성 성공: userId=${response.userId}, email=${request.email}" }
                response.userId
            }

            is KeycloakErrorResponse -> {
                logger.error { "Keycloak 사용자 생성 실패: email=${request.email}, error=${response.error}" }
                throw BusinessException(UserErrorCode.USER_CREATE_FAILED)
            }

            else -> {
                logger.error { "Keycloak 사용자 생성 응답 파싱 실패: email=${request.email}" }
                throw BusinessException(UserErrorCode.KEYCLOAK_PARSING_ERROR)
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
                is KeycloakDeleteUserResponse -> {
                    logger.info { "보상 트랜잭션 성공: Keycloak 사용자 삭제 완료 - userId=$keycloakUserId" }
                }

                is KeycloakErrorResponse -> {
                    logger.error {
                        "보상 트랜잭션 실패: Keycloak 사용자 삭제 오류 - userId=$keycloakUserId, error=${response.error}"
                    }
                    // 보상 트랜잭션 실패는 별도 알림/모니터링이 필요할 수 있음
                    notifyCompensationFailure(keycloakUserId, email, response.error)
                }

                else -> {
                    logger.error { "보상 트랜잭션 실패: 알 수 없는 응답 형식 - userId=$keycloakUserId" }
                    notifyCompensationFailure(keycloakUserId, email, "UNKNOWN_RESPONSE")
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