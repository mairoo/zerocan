package kr.pincoin.api.domain.user.model

import kr.pincoin.api.domain.user.model.enums.Role
import java.time.LocalDateTime

class User private constructor(
    // 1. 공통 불변 필드
    val id: Long? = null,
    val created: LocalDateTime? = null,
    val modified: LocalDateTime? = null,

    // 2. 공통 가변 필드
    val isRemoved: Boolean,

    // 3. 도메인 로직 불변 필드

    // 4. 도메인 로직 가변 필드
    val isActive: Boolean,
    val name: String,
    val email: String,

    // 역할 관리
    val roles: List<Role>,
) {
    init {
        require(!(isRemoved && isActive)) { "삭제된 사용자는 활성화될 수 없습니다" }
        require(roles.isNotEmpty()) { "사용자는 최소한 하나의 역할을 가져야 합니다" }
    }

    // 특정 역할을 가지고 있는지 확인
    fun hasRole(
        requiredRole: Role,
    ): Boolean =
        roles.contains(requiredRole)

    // 특정 역할들 중 하나라도 가지고 있는지 확인
    fun hasAnyRole(
        vararg requiredRoles: Role,
    ): Boolean =
        roles.any { requiredRoles.contains(it) }

    // 역할 업데이트
    fun updateRoles(
        newRoles: List<Role>,
    ): User {
        require(newRoles.isNotEmpty()) { "사용자는 최소한 하나의 역할을 가져야 합니다" }
        if (isRemoved) return this

        return copy(roles = newRoles)
    }

    fun activate(): User {
        if (isActive || isRemoved) return this
        return copy(isActive = true)
    }

    fun deactivate(): User {
        if (!isActive) return this
        return copy(isActive = false)
    }

    fun updateProfile(
        newName: String? = null,
        newEmail: String? = null,
    ): User {
        if (isRemoved) return this

        return copy(
            name = newName ?: name,
            email = newEmail ?: email,
        )
    }

    private fun copy(
        isRemoved: Boolean = this.isRemoved,
        isActive: Boolean = this.isActive,
        name: String = this.name,
        email: String = this.email,
        roles: List<Role> = this.roles,
    ): User = User(
        id = this.id,
        created = this.created,
        modified = this.modified,
        isRemoved = isRemoved,
        isActive = isActive,
        name = name,
        email = email,
        roles = roles,
    )

    companion object {
        fun of(
            id: Long? = null,
            created: LocalDateTime? = null,
            modified: LocalDateTime? = null,
            isRemoved: Boolean? = null,
            isActive: Boolean = true,
            name: String,
            email: String,
            roles: List<Role> = emptyList(),
        ): User {
            require(!((isRemoved ?: false) && isActive)) { "삭제된 사용자는 활성화될 수 없습니다" }

            return User(
                id = id,
                created = created,
                modified = modified,
                isRemoved = isRemoved ?: false,
                isActive = isActive,
                name = name,
                email = email,
                roles = roles,
            )
        }
    }
}