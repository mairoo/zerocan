package kr.pincoin.api.domain.user.model

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
) {
    init {
        require(!(isRemoved && isActive)) { "삭제된 사용자는 활성화될 수 없습니다" }
    }

    private fun copy(
        isRemoved: Boolean = this.isRemoved,
        isActive: Boolean = this.isActive,
        name: String = this.name,
        email: String = this.email,
    ): User = User(
        id = this.id,
        created = this.created,
        modified = this.modified,
        isRemoved = isRemoved,
        isActive = isActive,
        name = name,
        email = email,
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
            )
        }
    }
}