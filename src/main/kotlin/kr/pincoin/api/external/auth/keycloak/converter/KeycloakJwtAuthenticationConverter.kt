package kr.pincoin.api.external.auth.keycloak.converter

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.domain.user.repository.UserRepository
import kr.pincoin.api.infra.user.repository.criteria.UserSearchCriteria
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Keycloak JWT 토큰 검증을 위한 디코더
 *
 * ## 필요성
 * - Keycloak에서 발급한 JWT 토큰의 서명을 검증하여 토큰의 무결성과 신뢰성을 보장
 * - 위조되거나 변조된 토큰을 차단하여 애플리케이션 보안 강화
 *
 * ## 주요 역할
 * 1. **서명 검증**: Keycloak의 공개키로 JWT 서명을 검증하여 토큰이 신뢰할 수 있는 발급처에서 생성되었는지 확인
 * 2. **토큰 파싱**: JWT 헤더, 페이로드, 서명을 분리하여 클레임 정보를 추출 가능한 형태로 변환
 * 3. **만료 검증**: 토큰의 유효기간(exp)을 확인하여 만료된 토큰을 거부
 * 4. **표준 검증**: JWT 표준에 따른 기본 검증 수행 (iss, aud, iat 등)
 *
 * ## 적용된 패턴
 * - **Factory Pattern**: JwtDecoder 인스턴스 생성을 캡슐화하여 생성 로직을 분리
 * - **Builder Pattern**: NimbusJwtDecoder.withJwkSetUri()를 통해 단계적으로 디코더를 구성
 *
 * ## 동작 원리
 * ```
 * 1. Keycloak JWKS 엔드포인트에서 공개키 정보 획득
 * 2. 클라이언트가 전송한 JWT 토큰의 서명을 공개키로 검증
 * 3. 서명이 유효하면 토큰을 파싱하여 Spring Security가 사용할 수 있는 형태로 변환
 * 4. 서명이 무효하면 JwtException 발생
 * ```
 */
@Component
class KeycloakJwtAuthenticationConverter(
    private val userRepository: UserRepository,
) : Converter<Jwt, AbstractAuthenticationToken> {
    private val logger = KotlinLogging.logger {}

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val email = jwt.getClaimAsString("preferred_username")
            ?: jwt.getClaimAsString("email")
            ?: jwt.subject

        val authorities = getUserAuthorities(email)

        logger.debug { "JWT 인증 변환: email=$email, authorities=$authorities" }

        return JwtAuthenticationToken(jwt, authorities, email)
    }

    private fun getUserAuthorities(email: String?): Collection<GrantedAuthority> {
        if (email.isNullOrBlank()) {
            logger.warn { "JWT에서 이메일을 찾을 수 없음" }
            return emptyList()
        }

        return try {
            val user = userRepository.findUserWithRoles(
                UserSearchCriteria(email = email, isActive = true)
            )

            if (user != null) {
                val authorities = user.roles.map { role ->
                    SimpleGrantedAuthority(role.name)
                }
                logger.debug { "사용자 권한 조회 완료: email=$email, roles=${user.roles.map { it.name }}" }
                authorities
            } else {
                logger.warn { "사용자를 찾을 수 없음: email=$email" }
                listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
            }
        } catch (e: Exception) {
            logger.error(e) { "권한 조회 중 오류 발생: email=$email" }
            listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
        }
    }
}