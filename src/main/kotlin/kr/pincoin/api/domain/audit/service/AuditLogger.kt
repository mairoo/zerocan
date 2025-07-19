package kr.pincoin.api.domain.audit.service

import kr.pincoin.api.domain.audit.model.Auditable

interface AuditLogger {
    fun persist(entity: Auditable)

    fun update(entity: Auditable)

    fun delete(entity: Auditable)
}