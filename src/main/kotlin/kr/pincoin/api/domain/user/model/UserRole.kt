package kr.pincoin.api.domain.user.model

import kr.pincoin.api.domain.user.model.enums.Role
import java.time.LocalDateTime

class UserRole private constructor(
    // 1. 공통 불변 필드
    val id: Long? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,

    // 2. 공통 가변 필드
    val isRemoved: Boolean,

    // 3. 도메인 로직 불변 필드
    val userId: Long,

    // 4. 도메인 로직 가변 필드
    val role: Role,
) {
    fun remove(): UserRole {
        if (isRemoved) return this
        return copy(isRemoved = true)
    }

    fun restore(): UserRole {
        if (!isRemoved) return this
        return copy(isRemoved = false)
    }

    private fun copy(
        isRemoved: Boolean = this.isRemoved,
        role: Role = this.role
    ): UserRole = UserRole(
        id = this.id,
        created = this.created,
        modified = this.modified,
        isRemoved = isRemoved,
        userId = this.userId,
        role = role,
    )

    companion object {
        fun of(
            id: Long? = null,
            created: LocalDateTime? = null,
            modified: LocalDateTime? = null,
            isRemoved: Boolean? = null,
            userId: Long,
            role: Role,
        ): UserRole =
            UserRole(
                id = id,
                created = created,
                modified = modified,
                isRemoved = isRemoved ?: false,
                userId = userId,
                role = role,
            )
    }
}