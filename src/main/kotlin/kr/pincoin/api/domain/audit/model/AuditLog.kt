package kr.pincoin.api.domain.audit.model

import kr.pincoin.api.domain.audit.enums.AuditType
import java.time.LocalDateTime

class AuditLog private constructor(
    // 1. 공통 불변 필드
    val id: Long? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,
    val createdBy: Long? = null,
    val modifiedBy: Long? = null,

    // 2. 도메인 로직 불변 필드
    val entityType: String,
    val entityId: String,
    val type: AuditType,
    val origin: String,
    val changed: String?,
) {
    companion object {
        fun of(
            id: Long? = null,
            created: LocalDateTime? = null,
            modified: LocalDateTime? = null,
            createdBy: Long? = null,
            modifiedBy: Long? = null,
            entityType: String,
            entityId: String,
            type: AuditType,
            origin: String,
            changed: String? = null,
        ): AuditLog =
            AuditLog(
                id = id,
                created = created,
                modified = modified,
                createdBy = createdBy,
                modifiedBy = modifiedBy,
                entityType = entityType,
                entityId = entityId,
                type = type,
                origin = origin,
                changed = changed,
            )
    }
}