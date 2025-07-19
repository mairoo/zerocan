package kr.pincoin.api.domain.audit.event

import kr.pincoin.api.domain.audit.enums.AuditType

data class AuditLogEvent(
    val entityType: String,
    val entityId: String,
    val type: AuditType,
    val origin: String,
    val changed: String?
)