package kr.pincoin.api.infra.audit.mapper

import kr.pincoin.api.domain.audit.model.AuditLog
import kr.pincoin.api.infra.audit.entity.AuditLogEntity

fun AuditLogEntity?.toModel(): AuditLog? =
    this?.let { entity ->
        AuditLog.of(
            id = entity.id,
            created = entity.dateTimeFields.createdAt,
            modified = entity.dateTimeFields.modifiedAt,
            createdBy = entity.auditorFields.createdBy,
            modifiedBy = entity.auditorFields.modifiedBy,
            entityType = entity.entityType,
            entityId = entity.entityId,
            type = entity.type,
            origin = entity.origin,
            changed = entity.changed,
        )
    }

fun List<AuditLogEntity>?.toModelList(): List<AuditLog> =
    this?.mapNotNull { it.toModel() } ?: emptyList()

fun AuditLog?.toEntity(): AuditLogEntity? =
    this?.let { model ->
        AuditLogEntity.of(
            id = model.id,
            entityType = model.entityType,
            entityId = model.entityId,
            type = model.type,
            origin = model.origin,
            changed = model.changed,
            // created, modified, createdBy, modifiedBy: 매핑 안 함 JPA Auditing 관리 필드
        )
    }