package kr.pincoin.api.app.auth.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kr.pincoin.api.app.auth.request.SignInRequest
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.response.AccessTokenResponse
import kr.pincoin.api.app.auth.service.AuthService
import kr.pincoin.api.app.user.common.response.UserResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import kr.pincoin.api.global.constant.CookieKey
import kr.pincoin.api.global.response.success.ApiResponse
import kr.pincoin.api.global.utils.DomainUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val keycloakProperties: KeycloakProperties,
    private val authService: AuthService,
) {
    /**
     * 사용자 로그인을 처리하고 액세스 토큰과 리프레시 토큰을 발급
     */
    @PostMapping("/sign-in")
    fun signIn(
        @Valid @RequestBody request: SignInRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AccessTokenResponse>> {
        val tokenPair = authService.login(request)

        return ResponseEntity.ok()
            .headers(
                createRefreshTokenCookie(
                    refreshToken = tokenPair.refreshToken,
                    request = servletRequest,
                    rememberMe = tokenPair.rememberMe,
                    refreshExpiresIn = tokenPair.refreshExpiresIn,
                )
            )
            .body(ApiResponse.of(tokenPair.accessToken))
    }

    /**
     * 리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급
     */
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = CookieKey.REFRESH_TOKEN_NAME) refreshToken: String,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AccessTokenResponse>> {
        val tokenPair = authService.refreshToken(refreshToken)

        return ResponseEntity.ok()
            .headers(
                createRefreshTokenCookie(
                    refreshToken = tokenPair.refreshToken,
                    request = servletRequest,
                    rememberMe = tokenPair.rememberMe,
                    refreshExpiresIn = tokenPair.refreshExpiresIn,
                )
            )
            .body(ApiResponse.of(tokenPair.accessToken))
    }

    /**
     * 사용자 로그아웃을 처리하고 리프레시 토큰을 무효화
     */
    @PostMapping("/sign-out")
    fun signOut(
        @CookieValue(name = CookieKey.REFRESH_TOKEN_NAME, required = false) refreshToken: String?,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        refreshToken?.let { authService.logout(it) }

        return ResponseEntity.ok()
            .headers(
                createRefreshTokenCookie(
                    refreshToken = null,
                    request = servletRequest,
                    rememberMe = false,
                    refreshExpiresIn = null,
                )
            )
            .body(ApiResponse.of(Unit))
    }

    /**
     * 회원 가입
     */
    @PostMapping("/sign-up")
    fun signUp(
        @Valid @RequestBody request: SignUpRequest,
    ): ResponseEntity<ApiResponse<UserResponse>> =
        authService.createUser(request)
            .let { UserResponse.from(it) }
            .let { ApiResponse.of(it) }
            .let { ResponseEntity.ok(it) }

    /**
     * 리프레시 토큰을 포함하는 HTTP 쿠키 생성
     */
    private fun createRefreshTokenCookie(
        refreshToken: String?,
        request: HttpServletRequest,
        rememberMe: Boolean = false,
        refreshExpiresIn: Long? = null,
    ): HttpHeaders =
        HttpHeaders().apply {
            val cookieValue = refreshToken?.takeIf { it.isNotEmpty() }
            val requestDomain = DomainUtils.getRequestDomain(request)
            val cookieDomain = keycloakProperties.findCookieDomain(requestDomain)

            val cookie = ResponseCookie.from(CookieKey.REFRESH_TOKEN_NAME, cookieValue ?: "")
                .httpOnly(true)
                .secure(!requestDomain.contains("localhost"))
                .path(CookieKey.PATH)
                .maxAge(
                    when { // rememberMe와 실제 토큰 만료시간에 따른 쿠키 만료시간 설정
                        cookieValue == null -> 0 // 쿠키 삭제
                        rememberMe -> requireNotNull(refreshExpiresIn) { "refreshExpiresIn == null" }
                        else -> -1 // rememberMe=false: 세션 쿠키 (브라우저 종료시 삭제)
                    }
                )
                .sameSite(CookieKey.SAME_SITE)
                .domain(cookieDomain)
                .build()

            add(HttpHeaders.SET_COOKIE, cookie.toString())

            // 인증 API 보안 헤더
            add("X-Content-Type-Options", "nosniff") // MIME 스니핑 공격 방어
            add("X-Frame-Options", "DENY") // 클릭재킹 공격 방어

            // HTTPS 환경에서만 추가 보안 헤더 적용 (1년간 유효)
            if (request.isSecure) {
                add("Strict-Transport-Security", "max-age=31536000") // HTTPS 강제
            }
        }
}