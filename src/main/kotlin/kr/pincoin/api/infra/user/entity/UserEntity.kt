package kr.pincoin.api.infra.user.entity

import jakarta.persistence.*
import kr.pincoin.api.domain.audit.handler.AuditEntityListener
import kr.pincoin.api.domain.audit.model.Auditable
import kr.pincoin.api.infra.common.jpa.DateTimeFields
import kr.pincoin.api.infra.common.jpa.RemovalFields

@Entity
@Table(name = "auth_user")
@EntityListeners(AuditEntityListener::class)
class UserEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Embedded
    val dateTimeFields: DateTimeFields = DateTimeFields(),

    @Embedded
    val removalFields: RemovalFields = RemovalFields(),

    @Column(name = "keycloak_id")
    val keycloakId: String,

    @Column(name = "is_active")
    val isActive: Boolean,

    @Column(name = "name")
    val name: String,

    @Column(name = "email")
    val email: String,
) : Auditable {
    @Transient
    override var originState: Map<String, String?> =
        mapOf()

    override fun getState(): Map<String, String?> =
        mapOf(
            "id" to id?.toString(),
            "isActive" to isActive.toString(),
            "isRemoved" to removalFields.isRemoved.toString(),
            "name" to name,
            "email" to email,
        )

    override fun getEntityType(): String = "User"

    override fun getEntityId(): String = id?.toString() ?: ""

    companion object {
        fun of(
            id: Long? = null,
            isRemoved: Boolean = false,
            keycloakId: String,
            isActive: Boolean = true,
            name: String,
            email: String,
        ) = UserEntity(
            id = id,
            removalFields = RemovalFields().apply {
                this.isRemoved = isRemoved
            },
            keycloakId = keycloakId,
            isActive = isActive,
            name = name,
            email = email,
        )
    }
}