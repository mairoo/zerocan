package kr.pincoin.api.infra.audit.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.domain.audit.enums.AuditType
import kr.pincoin.api.domain.audit.event.AuditLogEvent
import kr.pincoin.api.domain.audit.model.AuditChange
import kr.pincoin.api.domain.audit.model.Auditable
import kr.pincoin.api.domain.audit.service.AuditLogger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class AuditLoggerImpl(
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher,
) : AuditLogger {
    override fun persist(entity: Auditable) {
        log(AuditType.CREATE, entity)
    }

    override fun update(entity: Auditable) {
        val changed = mutableListOf<AuditChange>()

        entity.getState().forEach { (key, value) ->
            if (entity.originState[key] != value) {
                changed.add(
                    AuditChange(
                        field = key,
                        oldValue = entity.originState[key],
                        newValue = value,
                    )
                )
            }
        }

        if (changed.isNotEmpty()) {
            log(AuditType.UPDATE, entity, changed)
        }
    }

    override fun delete(entity: Auditable) {
        log(AuditType.DELETE, entity)
    }

    private fun log(
        type: AuditType,
        entity: Auditable,
        changed: List<AuditChange>? = null,
    ) {
        val origin = if (type == AuditType.CREATE) entity.getState() else entity.originState

        eventPublisher.publishEvent(
            AuditLogEvent(
                entityType = entity.getEntityType(),
                entityId = entity.getEntityId(),
                type = type,
                origin = objectMapper.writeValueAsString(origin),
                changed = changed?.let { objectMapper.writeValueAsString(it) }
            )
        )
    }
}