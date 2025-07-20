package kr.pincoin.api.domain.user.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.user.admin.request.AdminUserCreateRequest
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
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createUser(
        request: AdminUserCreateRequest,
        keycloakId: String,
    ): User {
        logger.info { "관리자 사용자 생성 시작: email=${request.email}, role=${request.role}" }

        try {
            val user = User.of(
                isActive = true,
                keycloakId = keycloakId,
                name = request.name,
                email = request.email,
                roles = listOf(request.role),
            )

            val savedUser = userRepository.save(user)
            logger.info { "관리자 사용자 생성 완료: id=${savedUser.id}, roles=${savedUser.roles}" }

            return savedUser
        } catch (e: DataIntegrityViolationException) {
            logger.error { "사용자 중복: email=${request.email}" }
            throw BusinessException(UserErrorCode.ALREADY_EXISTS)
        }
    }

    @Transactional
    fun createUser(
        request: SignUpRequest,
        keycloakId: String,
    ): User {
        logger.info { "일반 사용자 생성 시작: email=${request.email}" }

        try {
            val user = User.of(
                isActive = true,
                keycloakId = keycloakId,
                name = request.name,
                email = request.email,
                roles = listOf(Role.ROLE_MEMBER),
            )

            logger.info { "User 도메인 객체 생성 완료: email=${user.email}, roles=${user.roles}" }

            val savedUser = userRepository.save(user)
            logger.info { "일반 사용자 생성 완료: id=${savedUser.id}, roles=${savedUser.roles}" }

            return savedUser
        } catch (e: DataIntegrityViolationException) {
            logger.error { "사용자 중복: email=${request.email}" }
            throw BusinessException(UserErrorCode.ALREADY_EXISTS)
        } catch (e: Exception) {
            logger.error { "사용자 생성 중 예외 발생: email=${request.email}, error=${e.message}" }
            throw e
        }
    }
}