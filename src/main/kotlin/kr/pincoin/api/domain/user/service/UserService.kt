package kr.pincoin.api.domain.user.service

import kr.pincoin.api.app.user.admin.request.AdminUserCreateRequest
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.domain.user.model.enums.Role
import kr.pincoin.api.domain.user.repository.UserRepository
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createUser(
        request: AdminUserCreateRequest,
        keycloakId: String,
    ): User {
        try {
            return userRepository.save(
                User.of(
                    isActive = true,
                    keycloakId = keycloakId,
                    name = request.name,
                    email = request.email,
                    roles = listOf(request.role),
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw BusinessException(UserErrorCode.ALREADY_EXISTS)
        }
    }

    @Transactional
    fun createUser(
        request: SignUpRequest,
        keycloakId: String,
    ): User {
        try {
            return userRepository.save(
                User.of(
                    isActive = true,
                    keycloakId = keycloakId,
                    name = request.name,
                    email = request.email,
                    roles = listOf(Role.ROLE_MEMBER),
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw BusinessException(UserErrorCode.ALREADY_EXISTS)
        }
    }
}