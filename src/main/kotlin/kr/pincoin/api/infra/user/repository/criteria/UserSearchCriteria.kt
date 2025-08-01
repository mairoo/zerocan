package kr.pincoin.api.infra.user.repository.criteria

data class UserSearchCriteria(
    val userId: Long? = null,
    val keycloakId: String? = null,
    val isActive: Boolean? = null,
    val name: String? = null,
    val email: String? = null,
)