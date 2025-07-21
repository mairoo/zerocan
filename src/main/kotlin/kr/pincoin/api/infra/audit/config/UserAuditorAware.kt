package kr.pincoin.api.infra.audit.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.domain.user.repository.UserRepository
import kr.pincoin.api.infra.user.repository.criteria.UserSearchCriteria
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAuditorAware(
    private val userRepository: UserRepository
) : AuditorAware<Long> {
    private val logger = KotlinLogging.logger {}

    override fun getCurrentAuditor(): Optional<Long> {
        return try {
            val authentication = SecurityContextHolder.getContext().authentication

            if (authentication?.isAuthenticated != true) {
                return Optional.empty()
            }

            when (val principal = authentication.principal) {
                is Jwt -> {
                    // JWT에서 사용자 정보 추출
                    val email = principal.getClaimAsString("preferred_username")
                        ?: principal.getClaimAsString("email")
                        ?: principal.subject

                    if (email.isNullOrBlank()) {
                        logger.warn { "JWT에서 사용자 식별 정보를 찾을 수 없음" }
                        return Optional.empty()
                    }

                    // 사용자 조회
                    val user = userRepository.findUser(
                        UserSearchCriteria(email = email, isActive = true)
                    )

                    if (user?.id != null) {
                        Optional.of(user.id)
                    } else {
                        logger.warn { "감사 로그용 사용자 조회 실패: email=$email" }
                        Optional.empty()
                    }
                }
                else -> {
                    logger.warn { "알 수 없는 Principal 타입: ${principal?.javaClass?.name}" }
                    Optional.empty()
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "현재 사용자 정보 조회 중 오류 발생" }
            Optional.empty()
        }
    }
}