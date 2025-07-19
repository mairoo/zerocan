package kr.pincoin.api.infra.audit.config

import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAuditorAware : AuditorAware<Long> {
    override fun getCurrentAuditor(
    ): Optional<Long> = Optional.empty()
}