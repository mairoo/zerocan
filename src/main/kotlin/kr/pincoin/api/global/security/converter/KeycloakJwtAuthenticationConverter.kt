package kr.pincoin.api.global.security.converter

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

@Component
class KeycloakJwtAuthenticationConverter(
    private val userRepository: UserRepository,
) : Converter<Jwt, AbstractAuthenticationToken> {

    private val logger = KotlinLogging.logger {}

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        // JWT에서 사용자 정보 추출
        val email = jwt.getClaimAsString("preferred_username")
            ?: jwt.getClaimAsString("email")
            ?: jwt.subject

        // 백엔드 DB에서 사용자의 실제 역할 조회
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
            // 백엔드 DB에서 사용자 조회
            val user = userRepository.findUser(
                UserSearchCriteria(email = email, isActive = true)
            )

            if (user != null) {
                // 사용자의 역할을 Spring Security 권한으로 변환
                val dbAuthorities = user.roles.map { role ->
                    SimpleGrantedAuthority("ROLE_${role.name}")
                }

                logger.debug { "DB에서 조회한 권한: email=$email, roles=${user.roles}" }
                dbAuthorities
            } else {
                logger.warn { "사용자를 찾을 수 없음: email=$email" }
                // 사용자가 DB에 없으면 기본 권한만 부여
                listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
            }
        } catch (e: Exception) {
            logger.error(e) { "권한 조회 중 오류 발생: email=$email" }
            // 에러 발생 시 기본 권한 부여
            listOf(SimpleGrantedAuthority("ROLE_MEMBER"))
        }
    }
}