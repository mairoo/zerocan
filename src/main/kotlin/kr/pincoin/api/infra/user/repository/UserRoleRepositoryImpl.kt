package kr.pincoin.api.infra.user.repository

import kr.pincoin.api.domain.user.model.UserRole
import kr.pincoin.api.domain.user.repository.UserRoleRepository
import kr.pincoin.api.infra.user.mapper.toEntity
import kr.pincoin.api.infra.user.mapper.toModel
import kr.pincoin.api.infra.user.mapper.toModelList
import kr.pincoin.api.infra.user.repository.criteria.UserRoleSearchCriteria
import org.springframework.stereotype.Repository

@Repository
class UserRoleRepositoryImpl(
    private val jpaRepository: UserRoleJpaRepository,
    private val queryRepository: UserRoleQueryRepository,
) : UserRoleRepository {
    override fun save(
        userRole: UserRole
    ): UserRole {
        return userRole.toEntity()
            ?.let { jpaRepository.save(it) }
            ?.toModel()
            ?: throw IllegalArgumentException("사용자 역할 저장 실패")
    }

    override fun findUserRoles(
        userId: Long,
    ): List<UserRole> =
        queryRepository.findUserRoles(userId).toModelList()

    override fun findUserRole(
        criteria: UserRoleSearchCriteria,
    ): UserRole? =
        queryRepository.findUserRole(criteria)?.toModel()

    override fun findUserRoles(
        criteria: UserRoleSearchCriteria,
    ): List<UserRole> =
        queryRepository.findUserRoles(criteria).toModelList()

    override fun existsUserRole(
        criteria: UserRoleSearchCriteria,
    ): Boolean =
        queryRepository.existsUserRole(criteria)
}