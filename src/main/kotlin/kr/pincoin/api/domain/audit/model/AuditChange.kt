package kr.pincoin.api.domain.audit.model

class AuditChange(
    val field: String,
    val oldValue: String?,
    val newValue: String?,
)