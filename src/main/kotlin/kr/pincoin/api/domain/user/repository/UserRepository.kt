package kr.pincoin.api.domain.user.repository

import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.infra.user.repository.projection.UserSearchCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepository {
    fun save(user: User): User

    fun findUser(
        userId: Long,
        criteria: UserSearchCriteria,
    ): User?

    fun findUser(
        criteria: UserSearchCriteria,
    ): User?

    fun findUsers(
        criteria: UserSearchCriteria,
        pageable: Pageable
    ): Page<User>

    fun existsByEmail(
        email: String,
    ): Boolean
}