package kr.pincoin.api.app.auth.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.domain.coordinator.user.UserResourceCoordinator
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.external.auth.keycloak.service.KeycloakAdminService
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.domain.user.error.UserErrorCode
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userResourceCoordinator: UserResourceCoordinator,
    private val keycloakAdminService: KeycloakAdminService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 사용자 회원가입
     * 1. Admin 토큰 획득
     * 2. Keycloak + DB 동시 사용자 생성 (보상 트랜잭션 포함)
     */
    fun createUser(request: SignUpRequest): User {
        logger.info { "회원가입 요청 시작: email=${request.email}" }

        return runBlocking {
            try {
                // 1. Admin 토큰 획득
                val adminToken = getAdminToken()

                // 2. 사용자 생성 (Keycloak + DB)
                val user = userResourceCoordinator.createUserWithKeycloak(request, adminToken)

                logger.info { "회원가입 완료: email=${request.email}, userId=${user.id}" }
                user

            } catch (e: BusinessException) {
                logger.error { "회원가입 비즈니스 오류: email=${request.email}, error=${e.errorCode}" }
                throw e
            } catch (e: Exception) {
                logger.error { "회원가입 시스템 오류: email=${request.email}, error=${e.message}" }
                throw BusinessException(UserErrorCode.SYSTEM_ERROR)
            }
        }
    }

    /**
     * Admin 토큰 획득
     * 실패시 적절한 예외로 변환
     */
    private suspend fun getAdminToken(): String {
        return when (val result = keycloakAdminService.getAdminToken()) {
            is KeycloakAdminService.AdminTokenResult.Success -> {
                logger.debug { "Admin 토큰 획득 성공" }
                result.accessToken
            }

            is KeycloakAdminService.AdminTokenResult.Error -> {
                logger.error { "Admin 토큰 획득 실패: ${result.errorCode} - ${result.errorMessage}" }
                throw BusinessException(UserErrorCode.ADMIN_TOKEN_FAILED)
            }
        }
    }
}