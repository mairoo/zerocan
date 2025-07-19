package kr.pincoin.api.domain.audit.handler

import kr.pincoin.api.domain.audit.event.AuditLogEvent
import kr.pincoin.api.domain.audit.model.AuditLog
import kr.pincoin.api.domain.audit.repository.AuditLogRepository
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class AuditLogEventHandler(
    private val auditLogRepository: AuditLogRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleAuditLogEvent(event: AuditLogEvent) {
        auditLogRepository.save(
            AuditLog.of(
                entityType = event.entityType,
                entityId = event.entityId,
                type = event.type,
                origin = event.origin,
                changed = event.changed
            )
        )
    }
}