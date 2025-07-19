package kr.pincoin.api.domain.audit.handler

import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import kr.pincoin.api.domain.audit.service.AuditLogger
import kr.pincoin.api.domain.audit.model.Auditable
import org.springframework.stereotype.Component

@Component
class AuditEntityListener(
    private var auditLogger: AuditLogger
) {
    @PostLoad
    fun postLoad(entity: Auditable) {
        entity.originState = entity.getState()
    }

    @PostPersist
    fun postPersist(entity: Auditable) {
        auditLogger.persist(entity)
        entity.originState = entity.getState()
    }

    @PostUpdate
    fun postUpdate(entity: Auditable) {
        auditLogger.update(entity)
        entity.originState = entity.getState()
    }

    @PostRemove
    fun postRemove(entity: Auditable) {
        auditLogger.delete(entity)
    }
}