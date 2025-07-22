// KeycloakJwtDecoder.kt
package kr.pincoin.api.external.auth.keycloak.decoder

import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

/**
 * Keycloak JWT를 Spring Security 인증 토큰으로 변환하는 컨버터
 *
 * ## 필요성
 * - Keycloak의 JWT 구조와 Spring Security의 인증 토큰 구조가 다르므로 중간 변환 계층이 필요
 * - 외부 시스템(Keycloak)과 내부 시스템(Spring Security) 간의 호환성을 제공
 * - JWT의 클레임 정보를 기반으로 애플리케이션별 권한을 동적으로 부여
 *
 * ## 주요 역할
 * 1. **사용자 식별**: JWT의 preferred_username, email, subject 클레임에서 사용자 이메일 추출
 * 2. **권한 매핑**: 데이터베이스에서 사용자 역할 정보를 조회하여 Spring Security 권한으로 변환
 * 3. **인증 토큰 생성**: JwtAuthenticationToken 객체로 변환하여 Spring Security Context에서 사용 가능하도록 함
 * 4. **예외 처리**: 사용자 조회 실패 시 기본 권한(ROLE_MEMBER) 부여하여 시스템 안정성 보장
 *
 * ## 적용된 패턴
 * - **Adapter Pattern**: Keycloak JWT 구조를 Spring Security가 요구하는 인터페이스로 적응시킴
 *   - Target Interface: AbstractAuthenticationToken (Spring Security가 기대하는 인터페이스)
 *   - Adaptee: Jwt (Keycloak이 제공하는 JWT 객체)
 *   - Adapter: KeycloakJwtAuthenticationConverter (두 시스템을 연결하는 어댑터)
 * - **Template Method Pattern**: Spring의 Converter<Jwt, AbstractAuthenticationToken> 인터페이스 구현
 *
 * ## 동작 흐름
 * ```
 * 1. JwtDecoder가 검증한 JWT 토큰을 입력으로 받음
 * 2. JWT 클레임에서 사용자 이메일 추출 (우선순위: preferred_username > email > subject)
 * 3. 이메일을 기반으로 데이터베이스에서 사용자 정보와 역할 조회
 * 4. 조회된 역할을 Spring Security의 GrantedAuthority로 변환
 * 5. JwtAuthenticationToken 생성하여 반환 (JWT 원본 + 권한 + 사용자명 포함)
 * ```
 *
 * ## 권한 부여 전략
 * - **성공 시**: 데이터베이스의 실제 사용자 역할을 권한으로 부여
 * - **사용자 없음**: ROLE_MEMBER 기본 권한 부여 (신규 사용자 고려)
 * - **조회 실패**: ROLE_MEMBER 기본 권한 부여 (시스템 안정성 우선)
 */
@Component
class KeycloakJwtDecoder(
    private val keycloakProperties: KeycloakProperties
) {
    fun createDecoder(): JwtDecoder =
        NimbusJwtDecoder
            .withJwkSetUri("${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/certs")
            .build()
}