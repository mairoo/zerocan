package kr.pincoin.api.domain.user.model.enums

enum class Role(
    val value: String,
) {
    ROLE_SYS("ROLE_SYS"), // 최고관리자
    ROLE_ADMIN("ROLE_ADMIN"), // 관리자
    ROLE_STAFF("ROLE_STAFF"), // 담당자
    ROLE_MEMBER("ROLE_MEMBER"), // 회원
    ROLE_API("ROLE_API"); // API 봇 (가상)

    override fun toString(): String {
        return value
    }

    companion object {
        fun from(value: String): Role? = entries.find { it.value == value }

        val ALLOWED_USER_ROLES = setOf(
            ROLE_ADMIN,
            ROLE_MEMBER,
        )
    }
}