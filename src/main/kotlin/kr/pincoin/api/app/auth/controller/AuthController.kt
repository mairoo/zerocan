package kr.pincoin.api.app.auth.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kr.pincoin.api.app.auth.request.SignInRequest
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.response.AccessTokenResponse
import kr.pincoin.api.app.auth.service.AuthService
import kr.pincoin.api.app.user.common.response.UserResponse
import kr.pincoin.api.global.constant.CookieKey
import kr.pincoin.api.global.properties.JwtProperties
import kr.pincoin.api.global.response.success.ApiResponse
import kr.pincoin.api.global.utils.DomainUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val jwtProperties: JwtProperties,
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
        val tokenPair = authService.login(request, servletRequest)

        return ResponseEntity.ok()
            .headers(createRefreshTokenCookie(tokenPair.refreshToken, servletRequest))
            .body(ApiResponse.of(tokenPair.accessToken))
    }

    // refresh

    // sign-out

    /**
     * 회원 가입을 합니다.
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
    ): HttpHeaders =
        HttpHeaders().apply {
            val cookieValue = refreshToken?.takeIf { it.isNotEmpty() }
            val requestDomain = DomainUtils.getRequestDomain(request)
            val cookieDomain = jwtProperties.findCookieDomain(requestDomain)

            val cookie = ResponseCookie.from(CookieKey.REFRESH_TOKEN_NAME, cookieValue ?: "")
                .httpOnly(true)
                .secure(!requestDomain.contains("localhost"))
                .path(CookieKey.PATH)
                .maxAge(cookieValue?.let { jwtProperties.refreshTokenExpiresIn } ?: 0)
                .sameSite(CookieKey.SAME_SITE)
                .domain(cookieDomain)
                .build()

            add(HttpHeaders.SET_COOKIE, cookie.toString())
        }
}