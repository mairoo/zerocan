package kr.pincoin.api.infra.user.repository

import kr.pincoin.api.infra.user.entity.UserRoleEntity
import kr.pincoin.api.infra.user.repository.criteria.UserRoleSearchCriteria

interface UserRoleQueryRepository {
    fun findUserRoles(
        userId: Long,
    ): List<UserRoleEntity>

    fun findUserRole(
        criteria: UserRoleSearchCriteria,
    ): UserRoleEntity?

    fun findUserRoles(
        criteria: UserRoleSearchCriteria,
    ): List<UserRoleEntity>

    fun existsUserRole(
        criteria: UserRoleSearchCriteria,
    ): Boolean
}