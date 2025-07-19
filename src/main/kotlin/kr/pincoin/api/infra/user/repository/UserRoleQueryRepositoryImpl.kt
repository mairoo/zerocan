package kr.pincoin.api.infra.user.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.pincoin.api.domain.user.model.enums.Role
import kr.pincoin.api.infra.user.entity.QUserRoleEntity
import kr.pincoin.api.infra.user.entity.UserRoleEntity
import kr.pincoin.api.infra.user.repository.criteria.UserRoleSearchCriteria
import org.springframework.stereotype.Repository

@Repository
class UserRoleQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserRoleQueryRepository {
    private val userRole = QUserRoleEntity.userRoleEntity

    override fun findUserRoles(
        userId: Long,
    ): List<UserRoleEntity> =
        queryFactory
            .selectFrom(userRole)
            .where(
                eqUserId(userId),
                eqIsRemoved(false)
            )
            .orderBy(userRole.id.desc())
            .fetch()

    override fun findUserRoles(
        userIds: List<Long>,
    ): List<UserRoleEntity> =
        queryFactory
            .selectFrom(userRole)
            .where(
                userRole.userId.`in`(userIds),
                eqIsRemoved(false),
            )
            .fetch()

    override fun findUserRole(
        criteria: UserRoleSearchCriteria,
    ): UserRoleEntity? =
        queryFactory
            .selectFrom(userRole)
            .where(*getCommonWhereConditions(criteria))
            .fetchOne()

    override fun findUserRoles(
        criteria: UserRoleSearchCriteria,
    ): List<UserRoleEntity> =
        queryFactory
            .selectFrom(userRole)
            .where(*getCommonWhereConditions(criteria))
            .orderBy(userRole.id.desc())
            .fetch()

    override fun existsUserRole(
        criteria: UserRoleSearchCriteria,
    ): Boolean =
        queryFactory
            .selectOne()
            .from(userRole)
            .where(*getCommonWhereConditions(criteria))
            .fetchFirst() != null

    private fun getCommonWhereConditions(
        criteria: UserRoleSearchCriteria,
    ): Array<BooleanExpression?> = arrayOf(
        eqId(criteria.id),
        eqUserId(criteria.userId),
        eqRole(criteria.role),
        eqIsRemoved(criteria.isRemoved),
    )

    private fun eqId(
        id: Long?,
    ): BooleanExpression? =
        id?.let { userRole.id.eq(it) }

    private fun eqUserId(
        userId: Long?,
    ): BooleanExpression? =
        userId?.let { userRole.userId.eq(it) }

    private fun eqRole(
        role: Role?,
    ): BooleanExpression? =
        role?.let { userRole.role.eq(it) }

    private fun eqIsRemoved(
        isRemoved: Boolean?,
    ): BooleanExpression? =
        isRemoved?.let { userRole.removalFields.isRemoved.eq(it) }
}