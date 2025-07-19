package kr.pincoin.api.infra.user.repository

import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.domain.user.model.UserRole
import kr.pincoin.api.domain.user.repository.UserRepository
import kr.pincoin.api.domain.user.repository.UserRoleRepository
import kr.pincoin.api.infra.user.mapper.toEntity
import kr.pincoin.api.infra.user.mapper.toModel
import kr.pincoin.api.infra.user.repository.criteria.UserRoleSearchCriteria
import kr.pincoin.api.infra.user.repository.criteria.UserSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
    private val queryRepository: UserQueryRepository,
    private val userRoleRepository: UserRoleRepository,
) : UserRepository {

    @Transactional
    override fun save(user: User): User {
        // 1. 사용자 엔티티 저장
        val savedEntity = user.toEntity()
            ?.let { jpaRepository.save(it) }
            ?: throw IllegalArgumentException("사용자 저장 실패")

        val userId = savedEntity.id ?: throw IllegalStateException("사용자 ID 생성 실패")

        // 2. 역할 처리
        user.roles.forEach { role ->
            // 이미 활성화된 동일한 역할이 있는지 확인
            val criteria = UserRoleSearchCriteria(
                userId = userId,
                role = role,
                isRemoved = false
            )

            if (!userRoleRepository.existsUserRole(criteria)) {
                // 소프트 삭제된 동일한 역할이 있는지 확인
                val deletedRoleCriteria = UserRoleSearchCriteria(
                    userId = userId,
                    role = role,
                    isRemoved = true
                )

                val deletedRole = userRoleRepository.findUserRole(deletedRoleCriteria)

                if (deletedRole != null) {
                    // 소프트 삭제된 역할이 있으면 재활성화
                    userRoleRepository.save(deletedRole.restore())
                } else {
                    // 없으면 새 역할 생성
                    userRoleRepository.save(UserRole.of(userId = userId, role = role))
                }
            }
        }

        // 3. 최신 사용자 정보 조회 및 반환 (역할 포함)
        return findUser(userId, UserSearchCriteria())
            ?: throw IllegalStateException("저장된 사용자를 조회할 수 없습니다")
    }

    override fun findUser(
        userId: Long,
        criteria: UserSearchCriteria,
    ): User? {
        val userEntity = queryRepository.findUser(userId, criteria) ?: return null
        val userRoles = userRoleRepository.findUserRoles(userId)
        val roles = userRoles.map { it.role }

        val user = userEntity.toModel() ?: return null
        return if (roles.isEmpty()) user else user.updateRoles(roles)
    }

    override fun findUser(
        criteria: UserSearchCriteria,
    ): User? {
        // select 쿼리 2회로 사용자 정보와 역할 목록 조회
        val userEntity = queryRepository.findUser(criteria) ?: return null
        val userRoles = userRoleRepository.findUserRoles(userEntity.id ?: return null)
        val roles = userRoles.map { it.role }

        val user = userEntity.toModel() ?: return null
        return if (roles.isEmpty()) user else user.updateRoles(roles)
    }

    override fun findUsers(
        criteria: UserSearchCriteria,
        pageable: Pageable,
    ): Page<User> {
        val userEntityPage = queryRepository.findUsers(criteria, pageable)
        if (userEntityPage.isEmpty) return Page.empty(pageable)

        // 모든 사용자의 역할을 한 번에 조회 (성능 최적화)
        val userIds = userEntityPage.content.mapNotNull { it.id }
        val userRoles = userRoleRepository.findUserRoles(userIds)
        val userRolesByUserId = userRoles.groupBy { it.userId }

        val users = userEntityPage.content.mapNotNull { entity ->
            val user = entity.toModel() ?: return@mapNotNull null
            val roles = userRolesByUserId[entity.id]?.map { it.role } ?: emptyList()

            if (roles.isEmpty()) user else user.updateRoles(roles)
        }

        return PageImpl(
            users,
            pageable,
            userEntityPage.totalElements
        )
    }

    override fun existsByEmail(
        email: String,
    ): Boolean =
        queryRepository.existsByEmail(email)
}