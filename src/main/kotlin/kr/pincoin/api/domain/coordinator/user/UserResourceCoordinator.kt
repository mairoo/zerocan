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
     * 트랜잭션 내에서 Keycloak 생성 실패시 롤백 처리
     */
    @Transactional
    suspend fun createUserWithKeycloak(
        request: SignUpRequest,
        adminToken: String
    ): User = withContext(Dispatchers.IO) {
        logger.info { "${"사용자 생성 시작: email={}"} ${request.email}" }

        try {
            // 1. Keycloak에 사용자 생성
            val keycloakUserId = createKeycloakUser(request, adminToken)

            // 2. DB에 사용자 생성 (Keycloak ID 연결)
            val user = userService.createUser(request, keycloakUserId)

            logger.info { "${"사용자 생성 완료: keycloakId={}, dbUserId={}"} $keycloakUserId ${user.id}" }
            user

        } catch (e: BusinessException) {
            logger.error { "${"사용자 생성 비즈니스 오류: email={}"} ${request.email} $e" }
            throw e
        } catch (e: Exception) {
            logger.error { "${"사용자 생성 시스템 오류: email={}"} ${request.email} $e" }
            throw BusinessException(UserErrorCode.SYSTEM_ERROR)
        }
    }

    /**
     * 사용자 인증 및 토큰 발급
     */
    suspend fun authenticateUser(email: String, password: String): AccessTokenResponse = withContext(Dispatchers.IO) {
        logger.info { "${"사용자 인증 시작: email={}"} $email" }

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
                logger.info { "${"사용자 인증 성공: email={}"} $email" }
                AccessTokenResponse.of(
                    accessToken = response.accessToken,
                    expiresIn = response.expiresIn
                )
            }

            is KeycloakErrorResponse -> {
                logger.error { "${"사용자 인증 실패: email={}, error={}"} $email ${response.error}" }
                throw BusinessException(UserErrorCode.AUTHENTICATION_FAILED)
            }

            else -> {
                logger.error { "${"사용자 인증 응답 파싱 실패: email={}"} $email" }
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
        logger.info { "${"Keycloak 사용자 생성 시작: email={}"} ${request.email}" }

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
                logger.info { "${"Keycloak 사용자 생성 성공: userId={}, email={}"} ${response.userId} ${request.email}" }
                response.userId
            }

            is KeycloakErrorResponse -> {
                logger.error { "${"Keycloak 사용자 생성 실패: email={}, error={}"} ${request.email} ${response.error}" }
                throw BusinessException(UserErrorCode.USER_CREATE_FAILED)
            }

            else -> {
                logger.error { "${"Keycloak 사용자 생성 응답 파싱 실패: email={}"} ${request.email}" }
                throw BusinessException(UserErrorCode.KEYCLOAK_PARSING_ERROR)
            }
        }
    }
}