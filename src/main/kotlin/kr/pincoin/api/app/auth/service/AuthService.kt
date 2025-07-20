package kr.pincoin.api.app.auth.service

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import kr.pincoin.api.app.auth.request.SignInRequest
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.response.AccessTokenResponse
import kr.pincoin.api.app.auth.vo.TokenPair
import kr.pincoin.api.domain.coordinator.user.UserResourceCoordinator
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakTokenResponse
import kr.pincoin.api.external.auth.keycloak.service.KeycloakAdminService
import kr.pincoin.api.external.auth.keycloak.service.KeycloakLoginService
import kr.pincoin.api.global.constant.RedisKey
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.global.utils.IpUtils
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthService(
    private val userResourceCoordinator: UserResourceCoordinator,
    private val keycloakAdminService: KeycloakAdminService,
    private val keycloakLoginService: KeycloakLoginService,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 로그인
     * 1. Keycloak 로그인으로 토큰 획득
     * 2. Redis에 토큰 TTL 저장
     * 3. 액세스 토큰은 JSON 응답, 리프레시 토큰은 쿠키용으로 반환
     */
    fun login(
        request: SignInRequest,
        servletRequest: HttpServletRequest,
    ): TokenPair {
        logger.info { "로그인 요청 시작: email=${request.email}" }

        return runBlocking {
            try {
                // 1. Keycloak 로그인
                val loginResult = keycloakLoginService.login(
                    username = request.email,
                    password = request.password
                )

                when (loginResult) {
                    is KeycloakLoginService.LoginResult.Success -> {
                        val tokenResponse = loginResult.tokenResponse

                        // 2. Redis에 토큰 저장 (TTL 설정)
                        storeTokensInRedis(tokenResponse, request.email, servletRequest)

                        // 3. 응답 구성
                        val accessTokenResponse = AccessTokenResponse.of(
                            accessToken = tokenResponse.accessToken,
                            expiresIn = tokenResponse.expiresIn
                        )

                        val refreshToken = if (request.rememberMe) {
                            tokenResponse.refreshToken
                        } else {
                            null // rememberMe가 false면 리프레시 토큰 쿠키 설정 안함
                        }

                        val tokenPair = TokenPair(
                            accessToken = accessTokenResponse,
                            refreshToken = refreshToken
                        )

                        logger.info { "로그인 성공: email=${request.email}" }
                        tokenPair
                    }

                    is KeycloakLoginService.LoginResult.Error -> {
                        logger.warn { "로그인 실패: email=${request.email}, error=${loginResult.errorCode}" }

                        // Keycloak 에러를 비즈니스 예외로 변환
                        val errorCode = when (loginResult.errorCode) {
                            "invalid_grant" -> UserErrorCode.INVALID_CREDENTIALS
                            "invalid_client" -> UserErrorCode.INVALID_CREDENTIALS
                            "TIMEOUT" -> UserErrorCode.LOGIN_TIMEOUT
                            else -> UserErrorCode.LOGIN_FAILED
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
     * 사용자 회원가입
     * 1. Admin 토큰 획득
     * 2. Keycloak + DB 동시 사용자 생성 (보상 트랜잭션 포함)
     */
    fun createUser(request: SignUpRequest): User {
        logger.info { "회원가입 요청 시작: email=${request.email}" }

        return runBlocking {
            try {
                // 1. Admin 토큰 획득
                val adminToken = getAdminToken()

                // 2. 사용자 생성 (Keycloak + DB)
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
     * 리프레시 토큰 Redis 저장
     */
    /**
     * 리프레시 토큰 Redis 저장
     * 액세스 토큰은 저장하지 않고 리프레시 토큰만 저장
     */
    private fun storeTokensInRedis(
        tokenResponse: KeycloakTokenResponse,
        email: String,
        request: HttpServletRequest,
    ) {
        val refreshToken = tokenResponse.refreshToken ?: return
        val clientIp = IpUtils.getClientIp(request)

        // 리프레시 토큰 정보만 저장
        redisTemplate.opsForHash<String, String>().putAll(
            refreshToken,
            mapOf(
                RedisKey.EMAIL to email,
                RedisKey.IP_ADDRESS to clientIp,
                "issued_at" to System.currentTimeMillis().toString()
            )
        )

        redisTemplate.expire(refreshToken, tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)
    }

    /**
     * Admin 토큰 획득
     * 실패시 적절한 예외로 변환
     */
    private suspend fun getAdminToken(): String {
        return when (val result = keycloakAdminService.getAdminToken()) {
            is KeycloakAdminService.AdminTokenResult.Success -> {
                logger.debug { "Admin 토큰 획득 성공" }
                result.accessToken
            }

            is KeycloakAdminService.AdminTokenResult.Error -> {
                logger.error { "Admin 토큰 획득 실패: ${result.errorCode} - ${result.errorMessage}" }
                throw BusinessException(UserErrorCode.ADMIN_TOKEN_FAILED)
            }
        }
    }
}