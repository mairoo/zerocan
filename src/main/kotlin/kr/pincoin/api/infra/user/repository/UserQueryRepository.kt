package kr.pincoin.api.infra.user.repository

import kr.pincoin.api.infra.user.entity.UserEntity
import kr.pincoin.api.infra.user.repository.criteria.UserSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserQueryRepository {
    fun findUser(
        userId: Long,
        criteria: UserSearchCriteria,
    ): UserEntity?

    fun findUser(
        criteria: UserSearchCriteria,
    ): UserEntity?

    fun findUsers(
        criteria: UserSearchCriteria,
        pageable: Pageable,
    ): Page<UserEntity>

    fun existsByEmail(
        email: String,
    ): Boolean

    fun existsByKeycloakId(
        keycloakId: String,
    ): Boolean
}