package kr.pincoin.api.infra.audit.config

import kr.pincoin.api.global.security.adapter.UserDetailsAdapter
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAuditorAware : AuditorAware<Long> {
    override fun getCurrentAuditor(
    ): Optional<Long> =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal
            ?.let { principal ->
                when (principal) {
                    is UserDetailsAdapter -> Optional.of(principal.user.id ?: -1L)
                    else -> Optional.empty()
                }
            } ?: Optional.empty()
}