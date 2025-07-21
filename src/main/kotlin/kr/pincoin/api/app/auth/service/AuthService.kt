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
import kr.pincoin.api.external.auth.keycloak.service.KeycloakTokenService
import kr.pincoin.api.global.constant.RedisKey
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.global.utils.IpUtils
import kr.pincoin.api.global.utils.JwtUtils
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AuthService(
    private val userResourceCoordinator: UserResourceCoordinator,
    private val keycloakAdminService: KeycloakAdminService,
    private val keycloakTokenService: KeycloakTokenService,
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtUtils: JwtUtils,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 로그인
     * 1. Keycloak 로그인으로 토큰 획득
     * 2. Redis에 세션 메타데이터만 저장 (토큰 자체는 저장하지 않음)
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
                val tokenResult = keycloakTokenService.login(
                    username = request.email,
                    password = request.password
                )

                when (tokenResult) {
                    is KeycloakTokenService.TokenResult.Success -> {
                        val tokenResponse = tokenResult.tokenResponse

                        // 2. Redis에 세션 메타데이터 저장 (토큰 자체는 저장하지 않음)
                        storeSessionMetadata(tokenResponse, request.email, servletRequest)

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

                    is KeycloakTokenService.TokenResult.Error -> {
                        logger.warn { "로그인 실패: email=${request.email}, error=${tokenResult.errorCode}" }

                        // Keycloak 에러를 비즈니스 예외로 변환
                        val errorCode = when (tokenResult.errorCode) {
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
     * 리프레시 토큰으로 액세스 토큰 갱신
     * 1. Keycloak에서 토큰 유효성 검증 및 갱신
     * 2. Redis 세션 메타데이터 업데이트
     */
    fun refreshToken(
        refreshToken: String,
        servletRequest: HttpServletRequest
    ): TokenPair {
        logger.info { "토큰 갱신 요청 시작" }

        return runBlocking {
            try {
                // 1. Keycloak에서 토큰 갱신
                val refreshResult = keycloakTokenService.refreshToken(refreshToken)

                when (refreshResult) {
                    is KeycloakTokenService.TokenResult.Success -> {
                        val tokenResponse = refreshResult.tokenResponse

                        // 2. 기존 세션 메타데이터 업데이트
                        updateSessionMetadata(refreshToken, tokenResponse, servletRequest)

                        val accessTokenResponse = AccessTokenResponse.of(
                            accessToken = tokenResponse.accessToken,
                            expiresIn = tokenResponse.expiresIn
                        )

                        val tokenPair = TokenPair(
                            accessToken = accessTokenResponse,
                            refreshToken = tokenResponse.refreshToken
                        )

                        logger.info { "토큰 갱신 성공" }
                        tokenPair
                    }

                    is KeycloakTokenService.TokenResult.Error -> {
                        logger.warn { "토큰 갱신 실패: error=${refreshResult.errorCode}" }

                        // 갱신 실패시 세션 메타데이터 제거
                        removeSessionMetadata(refreshToken)

                        val errorCode = when (refreshResult.errorCode) {
                            "invalid_grant" -> UserErrorCode.INVALID_REFRESH_TOKEN
                            "TIMEOUT" -> UserErrorCode.TOKEN_REFRESH_TIMEOUT
                            else -> UserErrorCode.TOKEN_REFRESH_FAILED
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
     * 로그아웃
     * 1. Keycloak에서 토큰 무효화
     * 2. Redis에서 세션 메타데이터 제거
     */
    fun logout(refreshToken: String) {
        logger.info { "로그아웃 요청 시작" }

        runBlocking {
            try {
                // 1. Keycloak에서 토큰 무효화
                keycloakTokenService.logout(refreshToken)

                // 2. Redis에서 세션 메타데이터 제거
                removeSessionMetadata(refreshToken)

                logger.info { "로그아웃 완료" }

            } catch (e: Exception) {
                logger.error { "로그아웃 오류: error=${e.message}" }
                // 로그아웃은 실패해도 클라이언트에게는 성공으로 응답
                // 세션 메타데이터만이라도 제거
                removeSessionMetadata(refreshToken)
            }
        }
    }

    /**
     * 특정 사용자의 모든 세션 무효화
     */
    fun logoutAllSessions(email: String) {
        logger.info { "전체 세션 로그아웃 요청: email=$email" }

        runBlocking {
            try {
                // Redis에서 해당 사용자의 모든 세션 찾기
                val pattern = "${RedisKey.SESSION_PREFIX}*"
                val sessionKeys = redisTemplate.keys(pattern)

                sessionKeys.forEach { sessionKey ->
                    val sessionEmail = redisTemplate.opsForHash<String, String>()
                        .get(sessionKey, RedisKey.EMAIL)

                    if (sessionEmail == email) {
                        // 개별 세션의 refresh token으로 Keycloak 로그아웃 시도
                        val refreshToken = redisTemplate.opsForHash<String, String>()
                            .get(sessionKey, RedisKey.REFRESH_TOKEN_REFERENCE)

                        if (!refreshToken.isNullOrBlank()) {
                            try {
                                keycloakTokenService.logout(refreshToken)
                            } catch (e: Exception) {
                                logger.warn { "개별 세션 Keycloak 로그아웃 실패: sessionKey=$sessionKey, error=${e.message}" }
                            }
                        }

                        // Redis에서 세션 메타데이터 제거
                        redisTemplate.delete(sessionKey)
                    }
                }

                logger.info { "전체 세션 로그아웃 완료: email=$email" }

            } catch (e: Exception) {
                logger.error { "전체 세션 로그아웃 오류: email=$email, error=${e.message}" }
                throw BusinessException(UserErrorCode.LOGOUT_ALL_FAILED)
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
     * 세션 메타데이터를 Redis에 저장
     * 토큰 자체는 저장하지 않고 JWT ID(jti)를 키로 사용하여 메타데이터만 저장
     */
    private fun storeSessionMetadata(
        tokenResponse: KeycloakTokenResponse,
        email: String,
        request: HttpServletRequest,
    ) {
        val refreshToken = tokenResponse.refreshToken ?: return

        try {
            // JWT의 jti(JWT ID) 추출
            val jti = jwtUtils.extractJti(refreshToken)
            val clientIp = IpUtils.getClientIp(request)
            val userAgent = request.getHeader("User-Agent") ?: ""
            val sessionKey = "${RedisKey.SESSION_PREFIX}$jti"

            // 세션 메타데이터 저장 (토큰 자체는 저장하지 않음)
            redisTemplate.opsForHash<String, String>().putAll(
                sessionKey,
                mapOf(
                    RedisKey.EMAIL to email,
                    RedisKey.IP_ADDRESS to clientIp,
                    RedisKey.USER_AGENT to userAgent,
                    RedisKey.ISSUED_AT to System.currentTimeMillis().toString(),
                    RedisKey.LAST_ACCESS_AT to System.currentTimeMillis().toString(),
                    // 참조용으로만 저장 (Keycloak 로그아웃시 필요)
                    RedisKey.REFRESH_TOKEN_REFERENCE to refreshToken
                )
            )

            // TTL 설정 (Keycloak의 refresh token 만료 시간과 동일)
            redisTemplate.expire(sessionKey, tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)

            logger.debug { "세션 메타데이터 저장 완료: email=$email, jti=$jti" }

        } catch (e: Exception) {
            logger.warn { "세션 메타데이터 저장 실패: email=$email, error=${e.message}" }
            // 메타데이터 저장 실패는 로그인을 막지 않음
        }
    }

    /**
     * 세션 메타데이터 업데이트 (토큰 갱신시)
     */
    private fun updateSessionMetadata(
        oldRefreshToken: String,
        newTokenResponse: KeycloakTokenResponse,
        request: HttpServletRequest
    ) {
        try {
            val oldJti = jwtUtils.extractJti(oldRefreshToken)
            val oldSessionKey = "${RedisKey.SESSION_PREFIX}$oldJti"

            // 기존 세션 정보 조회
            val existingSession = redisTemplate.opsForHash<String, String>().entries(oldSessionKey)

            if (existingSession.isNotEmpty()) {
                val newRefreshToken = newTokenResponse.refreshToken ?: return
                val newJti = jwtUtils.extractJti(newRefreshToken)
                val newSessionKey = "${RedisKey.SESSION_PREFIX}$newJti"

                // 현재 요청 정보 추출
                val currentIp = IpUtils.getClientIp(request)
                val currentUserAgent = request.getHeader("User-Agent") ?: ""
                val currentTime = System.currentTimeMillis().toString()

                // 새로운 세션 키로 데이터 이전 및 업데이트
                val updatedSession = existingSession.toMutableMap().apply {
                    // 기본 정보는 유지하되 갱신 시점의 정보로 업데이트
                    put(RedisKey.IP_ADDRESS, currentIp)
                    put(RedisKey.USER_AGENT, currentUserAgent)
                    put(RedisKey.LAST_ACCESS_AT, currentTime)
                    put(RedisKey.REFRESH_TOKEN_REFERENCE, newRefreshToken)

                    // 갱신 횟수 추가 (선택사항)
                    val refreshCount = (get(RedisKey.REFRESH_COUNT)?.toIntOrNull() ?: 0) + 1
                    put(RedisKey.REFRESH_COUNT, refreshCount.toString())
                }

                redisTemplate.opsForHash<String, String>().putAll(newSessionKey, updatedSession)
                redisTemplate.expire(newSessionKey, newTokenResponse.refreshExpiresIn, TimeUnit.SECONDS)

                // 기존 세션 데이터 제거
                redisTemplate.delete(oldSessionKey)

                logger.debug {
                    "세션 메타데이터 업데이트 완료: oldJti=$oldJti, newJti=$newJti, " +
                            "ip=$currentIp, userAgent=${currentUserAgent.take(50)}..."
                }
            }

        } catch (e: Exception) {
            logger.warn { "세션 메타데이터 업데이트 실패: error=${e.message}" }
        }
    }

    /**
     * 세션 메타데이터 제거
     */
    private fun removeSessionMetadata(refreshToken: String) {
        try {
            val jti = jwtUtils.extractJti(refreshToken)
            val sessionKey = "${RedisKey.SESSION_PREFIX}$jti"
            redisTemplate.delete(sessionKey)

            logger.debug { "세션 메타데이터 제거 완료: jti=$jti" }

        } catch (e: Exception) {
            logger.warn { "세션 메타데이터 제거 실패: error=${e.message}" }
        }
    }

    /**
     * 토큰 유효성 검증
     * 1. Keycloak에서 토큰 유효성 검증 (권위 있는 소스)
     * 2. Redis에서 세션 메타데이터 확인 (부가 검증)
     */
    suspend fun validateRefreshToken(refreshToken: String): Boolean {
        return try {
            // 1. 기본 JWT 형식 검증
            if (!jwtUtils.isValidFormat(refreshToken)) {
                logger.debug { "JWT 형식이 잘못됨" }
                return false
            }

            // 2. JWT 만료 확인
            if (jwtUtils.isTokenExpired(refreshToken)) {
                logger.debug { "JWT 토큰이 만료됨" }
                removeSessionMetadata(refreshToken)
                return false
            }

            // 3. Keycloak에서 실제 토큰 유효성 검증 (갱신 시도)
            val refreshResult = keycloakTokenService.refreshToken(refreshToken)
            val isValidInKeycloak = when (refreshResult) {
                is KeycloakTokenService.TokenResult.Success -> true
                is KeycloakTokenService.TokenResult.Error -> {
                    logger.debug { "Keycloak 토큰 검증 실패: ${refreshResult.errorCode}" }
                    false
                }
            }

            if (!isValidInKeycloak) {
                // Keycloak에서 무효하면 Redis에서도 제거
                removeSessionMetadata(refreshToken)
                return false
            }

            // 4. Redis에서 세션 메타데이터 확인
            val jti = jwtUtils.extractJti(refreshToken)
            val sessionKey = "${RedisKey.SESSION_PREFIX}$jti"
            val sessionExists = redisTemplate.hasKey(sessionKey)

            if (sessionExists) {
                // 마지막 접근 시간 업데이트
                redisTemplate.opsForHash<String, String>()
                    .put(sessionKey, RedisKey.LAST_ACCESS_AT, System.currentTimeMillis().toString())
            }

            sessionExists

        } catch (e: Exception) {
            logger.error { "토큰 검증 오류: error=${e.message}" }
            false
        }
    }

    /**
     * 사용자의 활성 세션 목록 조회
     */
    fun getActiveSessions(email: String): List<SessionInfo> {
        return try {
            val pattern = "${RedisKey.SESSION_PREFIX}*"
            val sessionKeys = redisTemplate.keys(pattern)

            sessionKeys.mapNotNull { sessionKey ->
                val sessionData = redisTemplate.opsForHash<String, String>().entries(sessionKey)

                if (sessionData[RedisKey.EMAIL] == email) {
                    SessionInfo(
                        sessionId = sessionKey.removePrefix(RedisKey.SESSION_PREFIX),
                        ipAddress = sessionData[RedisKey.IP_ADDRESS] ?: "",
                        userAgent = sessionData[RedisKey.USER_AGENT] ?: "",
                        issuedAt = sessionData[RedisKey.ISSUED_AT]?.toLongOrNull() ?: 0L,
                        lastAccessAt = sessionData[RedisKey.LAST_ACCESS_AT]?.toLongOrNull() ?: 0L
                    )
                } else null
            }

        } catch (e: Exception) {
            logger.error { "활성 세션 조회 오류: email=$email, error=${e.message}" }
            emptyList()
        }
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

    /**
     * 세션 정보 데이터 클래스
     */
    data class SessionInfo(
        val sessionId: String,
        val ipAddress: String,
        val userAgent: String,
        val issuedAt: Long,
        val lastAccessAt: Long
    )
}