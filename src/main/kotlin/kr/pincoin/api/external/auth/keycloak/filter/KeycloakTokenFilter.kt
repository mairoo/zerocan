package kr.pincoin.api.external.auth.keycloak.filter

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.external.auth.keycloak.exception.AuthenticationException
import kr.pincoin.api.global.response.error.ErrorResponse
import kr.pincoin.api.global.utils.JwtUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.PathContainer
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser

@Component
class KeycloakTokenFilter(
    private val jwtUtils: JwtUtils,
    private val userDetailsService: UserDetailsService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val requestPath = PathContainer.parsePath(request.requestURI)
        return publicPathPatterns.any { it.matches(requestPath) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val bearerToken = request.getBearerToken()

            // JWT 토큰 유효성 확인 및 이메일 추출 후 인증 처리
            bearerToken?.let {
                // 토큰 형식 확인
                if (!jwtUtils.isValidFormat(it)) {
                    throw AuthenticationException(UserErrorCode.INVALID_TOKEN)
                }

                // 토큰 만료 확인
                if (jwtUtils.isTokenExpired(it)) {
                    throw AuthenticationException(UserErrorCode.EXPIRED_TOKEN)
                }

                // 이메일 추출 및 인증 처리
                val email = jwtUtils.extractEmail(it)
                authenticateUser(email, request)
            }

            filterChain.doFilter(request, response)
        } catch (_: AuthenticationException) {
            SecurityContextHolder.clearContext()
            handleAuthenticationException(request, response)
        } catch (_: Exception) {
            SecurityContextHolder.clearContext()
            handleAuthenticationException(request, response)
        }
    }

    private fun HttpServletRequest.getBearerToken(): String? =
        getHeader(HttpHeaders.AUTHORIZATION)?.let { header ->
            // Header format
            // RFC 7235 standard header
            // Authorization: Bearer JWTString
            if (header.startsWith(BEARER_PREFIX)) {
                header.substring(BEARER_PREFIX.length).trim()
            } else null
        }

    private fun authenticateUser(email: String, request: HttpServletRequest) {
        try {
            // 1. 데이터베이스에서 email로 사용자 조회
            val userDetails = userDetailsService.loadUserByUsername(email)

            // 2. 인증 객체 생성
            val auth = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )

            // 3. 현재 인증된 사용자 정보를 보안 컨텍스트에 저장 = 로그인 처리
            SecurityContextHolder.getContext().authentication = auth

        } catch (_: UsernameNotFoundException) {
            // 인증 실패 시 상위 예외 핸들러에서 로깅하므로 예외만 변환하여 던짐
            throw AuthenticationException(UserErrorCode.INVALID_CREDENTIALS)
        }
    }

    private fun handleAuthenticationException(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.apply {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = "${MediaType.APPLICATION_JSON_VALUE};charset=UTF-8"
            characterEncoding = "UTF-8"
        }

        val errorResponse = ErrorResponse.of(
            request,
            UserErrorCode.INVALID_CREDENTIALS.status,
            UserErrorCode.INVALID_CREDENTIALS.message,
        )

        objectMapper.writeValue(response.writer, errorResponse)
    }

    companion object {
        const val BEARER_PREFIX = "Bearer "

        // JWT 토큰 검증이 불필요한 공개 엔드포인트 목록
        private val publicPaths = listOf(
            "/open/**",
            "/auth/**",
            "/actuator/**",
        )

        // PathPatternParser를 사용하여 패턴 파싱
        private val pathPatternParser = PathPatternParser()
        private val publicPathPatterns: List<PathPattern> = publicPaths.map {
            pathPatternParser.parse(it)
        }
    }
}