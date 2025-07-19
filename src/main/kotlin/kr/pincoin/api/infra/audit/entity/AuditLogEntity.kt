package kr.pincoin.api.infra.audit.entity

import jakarta.persistence.*
import kr.pincoin.api.domain.audit.enums.AuditType
import kr.pincoin.api.infra.common.jpa.AuditorFields
import kr.pincoin.api.infra.common.jpa.DateTimeFields
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener::class)
class AuditLogEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "entity_type")
    val entityType: String,

    @Column(name = "entity_id")
    val entityId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    val type: AuditType,

    @Column(name = "origin")
    val origin: String,

    @Column(name = "changed")
    val changed: String? = null,

    @Embedded
    val dateTimeFields: DateTimeFields = DateTimeFields(),

    @Embedded
    val auditorFields: AuditorFields = AuditorFields(),
) {
    companion object {
        fun of(
            id: Long? = null,
            entityType: String,
            entityId: String,
            type: AuditType,
            origin: String,
            changed: String? = null,
        ) = AuditLogEntity(
            id = id,
            entityType = entityType,
            entityId = entityId,
            type = type,
            origin = origin,
            changed = changed,
        )
    }
}