package kr.pincoin.api.app.auth.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kr.pincoin.api.app.auth.request.SignInRequest
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.response.AccessTokenResponse
import kr.pincoin.api.app.auth.vo.TokenPair
import kr.pincoin.api.domain.coordinator.user.UserResourceCoordinator
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakResponse
import kr.pincoin.api.external.auth.keycloak.error.KeycloakErrorCode
import kr.pincoin.api.external.auth.keycloak.service.KeycloakAdminService
import kr.pincoin.api.external.auth.keycloak.service.KeycloakTokenService
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userResourceCoordinator: UserResourceCoordinator,
    private val keycloakAdminService: KeycloakAdminService,
    private val keycloakTokenService: KeycloakTokenService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 로그인
     * OAuth2 리소스 서버 방식에서는 토큰만 반환
     * 세션 관리는 Keycloak이 담당
     */
    fun login(request: SignInRequest): TokenPair {
        logger.info { "로그인 요청 시작: email=${request.email}" }

        return runBlocking {
            try {
                // Keycloak 로그인으로 토큰 획득
                val tokenResult = keycloakTokenService.login(
                    username = request.email,
                    password = request.password
                )

                when (tokenResult) {
                    is KeycloakResponse.Success -> {
                        val tokenData = tokenResult.data

                        val accessTokenResponse = AccessTokenResponse.of(
                            accessToken = tokenData.accessToken,
                            expiresIn = tokenData.expiresIn
                        )

                        val refreshToken = if (request.rememberMe) {
                            tokenData.refreshToken
                        } else {
                            null
                        }

                        val tokenPair = TokenPair(
                            accessToken = accessTokenResponse,
                            refreshToken = refreshToken,
                            rememberMe = request.rememberMe,
                            refreshExpiresIn = if (request.rememberMe) tokenData.refreshExpiresIn else null
                        )

                        logger.info { "로그인 성공: email=${request.email}" }
                        tokenPair
                    }

                    is KeycloakResponse.Error -> {
                        logger.warn { "로그인 실패: email=${request.email}, error=${tokenResult.errorCode}" }

                        val errorCode = when (tokenResult.errorCode) {
                            "invalid_grant" -> KeycloakErrorCode.INVALID_CREDENTIALS
                            "invalid_client" -> KeycloakErrorCode.INVALID_CREDENTIALS
                            "TIMEOUT" -> KeycloakErrorCode.TIMEOUT
                            else -> KeycloakErrorCode.UNKNOWN
                        }

                        throw BusinessException(errorCode)
                    }
                }

            } catch (e: BusinessException) {
                logger.error { "로그인 비즈니스 오류: email=${request.email}, error=${e.errorCode}" }
                throw e
            } catch (e: Exception) {
                logger.error { "로그인 시스템 오류: email=${request.email}, error=${e.message}" }
                throw BusinessException(UserErrorCode.SYSTEM_ERROR)
            }
        }
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신
     */
    fun refreshToken(refreshToken: String): TokenPair {
        logger.info { "토큰 갱신 요청 시작" }

        return runBlocking {
            try {
                val refreshResult = keycloakTokenService.refreshToken(refreshToken)

                when (refreshResult) {
                    is KeycloakResponse.Success -> {
                        val tokenData = refreshResult.data

                        val accessTokenResponse = AccessTokenResponse.of(
                            accessToken = tokenData.accessToken,
                            expiresIn = tokenData.expiresIn
                        )

                        val tokenPair = TokenPair(
                            accessToken = accessTokenResponse,
                            refreshToken = tokenData.refreshToken,
                            rememberMe = true,
                            refreshExpiresIn = tokenData.refreshExpiresIn
                        )

                        logger.info { "토큰 갱신 성공" }
                        tokenPair
                    }

                    is KeycloakResponse.Error -> {
                        logger.warn { "토큰 갱신 실패: error=${refreshResult.errorCode}" }

                        val errorCode = when (refreshResult.errorCode) {
                            "invalid_grant" -> KeycloakErrorCode.INVALID_REFRESH_TOKEN
                            "TIMEOUT" -> KeycloakErrorCode.TIMEOUT
                            else -> KeycloakErrorCode.UNKNOWN
                        }

                        throw BusinessException(errorCode)
                    }
                }

            } catch (e: BusinessException) {
                logger.error { "토큰 갱신 비즈니스 오류: error=${e.errorCode}" }
                throw e
            } catch (e: Exception) {
                logger.error { "토큰 갱신 시스템 오류: error=${e.message}" }
                throw BusinessException(UserErrorCode.SYSTEM_ERROR)
            }
        }
    }

    /**
     * 로그아웃: Keycloak에서 토큰 무효화
     */
    fun logout(refreshToken: String) {
        logger.info { "로그아웃 요청 시작" }

        runBlocking {
            try {
                val logoutResult = keycloakTokenService.logout(refreshToken)

                when (logoutResult) {
                    is KeycloakResponse.Success -> {
                        logger.info { "로그아웃 성공" }
                    }

                    is KeycloakResponse.Error -> {
                        logger.warn { "Keycloak 로그아웃 실패: ${logoutResult.errorCode}" }
                        // 로그아웃 실패해도 클라이언트에게는 성공으로 응답
                    }
                }

            } catch (e: Exception) {
                logger.error { "로그아웃 오류: error=${e.message}" }
                // 로그아웃은 실패해도 클라이언트에게는 성공으로 응답
            }
        }
    }

    /**
     * 사용자 회원가입
     */
    fun createUser(request: SignUpRequest): User {
        logger.info { "회원가입 요청 시작: email=${request.email}" }

        return runBlocking {
            try {
                val adminToken = getAdminToken()
                val user = userResourceCoordinator.createUserWithKeycloak(request, adminToken)

                logger.info { "회원가입 완료: email=${request.email}, userId=${user.id}" }
                user

            } catch (e: BusinessException) {
                logger.error { "회원가입 비즈니스 오류: email=${request.email}, error=${e.errorCode}" }
                throw e
            } catch (e: Exception) {
                logger.error { "회원가입 시스템 오류: email=${request.email}, error=${e.message}" }
                throw BusinessException(UserErrorCode.SYSTEM_ERROR)
            }
        }
    }

    /**
     * Admin 토큰 획득
     */
    private suspend fun getAdminToken(): String {
        return when (val result = keycloakAdminService.getAdminToken()) {
            is KeycloakResponse.Success -> {
                logger.debug { "Admin 토큰 획득 성공" }
                result.data.accessToken
            }

            is KeycloakResponse.Error -> {
                logger.error { "Admin 토큰 획득 실패: ${result.errorCode} - ${result.errorMessage}" }
                throw BusinessException(KeycloakErrorCode.ADMIN_TOKEN_FAILED)
            }
        }
    }
}