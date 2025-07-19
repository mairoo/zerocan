package kr.pincoin.api.infra.user.entity

import jakarta.persistence.*
import kr.pincoin.api.domain.user.model.enums.Role
import kr.pincoin.api.infra.common.jpa.DateTimeFields
import kr.pincoin.api.infra.common.jpa.RemovalFields

@Entity
@Table(name = "auth_user_role")
class UserRoleEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Embedded
    val dateTimeFields: DateTimeFields = DateTimeFields(),

    @Embedded
    val removalFields: RemovalFields = RemovalFields(),

    @Column(name = "user_id")
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    val role: Role,
) {
    companion object {
        fun of(
            id: Long? = null,
            isRemoved: Boolean = false,
            userId: Long,
            role: Role,
        ) = UserRoleEntity(
            id = id,
            removalFields = RemovalFields().apply {
                this.isRemoved = isRemoved
            },
            userId = userId,
            role = role,
        )
    }
}