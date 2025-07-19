package kr.pincoin.api.infra.user.repository.projection

data class UserSearchCriteria(
    val userId: Long? = null,
    val isActive: Boolean? = null,
    val name: String? = null,
    val email: String? = null,
)
