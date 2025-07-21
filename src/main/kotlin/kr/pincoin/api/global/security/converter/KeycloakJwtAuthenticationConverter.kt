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
            val user = userRepository.findUser(
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