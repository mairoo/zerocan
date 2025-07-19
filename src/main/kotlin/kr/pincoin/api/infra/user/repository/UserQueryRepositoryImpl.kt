package kr.pincoin.api.infra.user.repository

import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.pincoin.api.infra.user.entity.QUserEntity
import kr.pincoin.api.infra.user.entity.UserEntity
import kr.pincoin.api.infra.user.repository.projection.UserSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository

@Repository
class UserQueryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserQueryRepository {
    private val user = QUserEntity.userEntity

    override fun findUser(
        userId: Long,
        criteria: UserSearchCriteria,
    ): UserEntity? =
        queryFactory
            .selectFrom(user)
            .where(
                eqId(userId),
                *getCommonWhereConditions(criteria)
            )
            .fetchOne()

    override fun findUser(
        criteria: UserSearchCriteria,
    ): UserEntity? {
        val identifierConditions = listOfNotNull(
            criteria.name,
            criteria.email
        )
        require(identifierConditions.isNotEmpty()) { "검색 조건을 하나 이상 지정해야 합니다." }

        return queryFactory
            .selectFrom(user)
            .where(*getCommonWhereConditions(criteria))
            .fetchOne()
    }

    override fun findUsers(
        criteria: UserSearchCriteria,
        pageable: Pageable
    ): Page<UserEntity> = executePageQuery(
        criteria,
        pageable
    ) { baseQuery -> baseQuery.select(user) }

    override fun existsByEmail(
        email: String,
    ): Boolean =
        queryFactory
            .selectOne()
            .from(user)
            .where(user.email.eq(email))
            .fetchFirst() != null

    private fun <T> executePageQuery(
        criteria: UserSearchCriteria,
        pageable: Pageable,
        selectClause: (JPAQuery<*>) -> JPAQuery<T>
    ): Page<T> {
        val whereConditions = getCommonWhereConditions(criteria)

        fun createBaseQuery() = queryFactory
            .from(user)
            .where(*whereConditions)

        val results = selectClause(createBaseQuery())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(user.id.desc())
            .fetch()

        val countQuery = {
            queryFactory
                .select(user.count())
                .from(user)
                .where(*whereConditions)
                .fetchOne() ?: 0L
        }

        return PageableExecutionUtils.getPage(
            results,
            pageable,
            countQuery,
        )
    }

    private fun getCommonWhereConditions(
        criteria: UserSearchCriteria
    ): Array<BooleanExpression?> = arrayOf(
        eqUserId(criteria.userId),
        eqIsActive(criteria.isActive),
        containsName(criteria.name),
        eqEmail(criteria.email),
    )

    private fun eqId(
        userId: Long?,
    ): BooleanExpression? =
        userId?.let { user.id.eq(it) }

    private fun eqUserId(
        userId: Long?,
    ): BooleanExpression? =
        userId?.let { user.id.eq(it) }

    private fun eqIsActive(
        isActive: Boolean?,
    ): BooleanExpression? =
        isActive?.let { user.isActive.eq(it) }

    private fun containsName(
        name: String?,
    ): BooleanExpression? =
        name?.let { user.name.contains(it) }

    private fun eqEmail(
        email: String?,
    ): BooleanExpression? =
        email?.let { user.email.eq(it) }
}