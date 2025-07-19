package kr.pincoin.api.infra.user.repository.criteria

import kr.pincoin.api.domain.user.model.enums.Role

data class UserRoleSearchCriteria(
    val id: Long? = null,
    val userId: Long? = null,
    val role: Role? = null,
    val isRemoved: Boolean? = null,
)