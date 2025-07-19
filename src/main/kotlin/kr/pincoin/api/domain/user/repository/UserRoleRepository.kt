package kr.pincoin.api.domain.user.repository

import kr.pincoin.api.domain.user.model.UserRole
import kr.pincoin.api.infra.user.repository.criteria.UserRoleSearchCriteria

interface UserRoleRepository {
    fun save(
        userRole: UserRole,
    ): UserRole

    fun findUserRoles(
        userId: Long,
    ): List<UserRole>

    fun findUserRole(
        criteria: UserRoleSearchCriteria,
    ): UserRole?

    fun findUserRoles(
        criteria: UserRoleSearchCriteria,
    ): List<UserRole>

    fun existsUserRole(
        criteria: UserRoleSearchCriteria,
    ): Boolean
}