package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakCreateUserRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakUpdateUserRequest
import kr.pincoin.api.external.auth.keycloak.api.response.*
import org.springframework.stereotype.Service

@Service
class KeycloakUserService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakAdminService: KeycloakAdminService,
) {
    /**
     * 새 사용자를 생성합니다.
     */
    suspend fun createUser(
        username: String,
        email: String,
        firstName: String,
        lastName: String,
        password: String? = null,
        enabled: Boolean = true,
    ): UserResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(15000) { // 15초
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakAdminService.AdminTokenResult.Success -> adminResult.accessToken
                    is KeycloakAdminService.AdminTokenResult.Error -> return@withTimeout UserResult.Error(
                        adminResult.errorCode,
                        adminResult.errorMessage
                    )
                }

                // 2. 사용자 생성 요청
                val credentials = password?.let {
                    listOf(KeycloakCreateUserRequest.KeycloakCredential(value = it))
                }

                val request = KeycloakCreateUserRequest(
                    username = username,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    enabled = enabled,
                    credentials = credentials
                )

                when (val response = keycloakApiClient.createUser(adminToken, request)) {
                    is KeycloakCreateUserResponse -> UserResult.Success(response.userId)
                    is KeycloakErrorResponse -> UserResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorMessage ?: response.errorDescription ?: "사용자 생성 실패"
                    )
                    else -> UserResult.Error("UNKNOWN", "알 수 없는 응답 형식")
                }
            }
        } catch (_: TimeoutCancellationException) {
            UserResult.Error("TIMEOUT", "사용자 생성 요청 시간 초과")
        } catch (e: Exception) {
            UserResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    /**
     * 사용자 정보를 조회합니다.
     */
    suspend fun getUser(userId: String): UserInfoResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakAdminService.AdminTokenResult.Success -> adminResult.accessToken
                    is KeycloakAdminService.AdminTokenResult.Error -> return@withTimeout UserInfoResult.Error(
                        adminResult.errorCode,
                        adminResult.errorMessage
                    )
                }

                // 2. 사용자 정보 조회
                when (val response = keycloakApiClient.getUser(adminToken, userId)) {
                    is KeycloakUserResponse -> UserInfoResult.Success(response)
                    is KeycloakErrorResponse -> UserInfoResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorMessage ?: response.errorDescription ?: "사용자 정보 조회 실패"
                    )
                    else -> UserInfoResult.Error("UNKNOWN", "알 수 없는 응답 형식")
                }
            }
        } catch (_: TimeoutCancellationException) {
            UserInfoResult.Error("TIMEOUT", "사용자 정보 조회 요청 시간 초과")
        } catch (e: Exception) {
            UserInfoResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    /**
     * 사용자 정보를 수정합니다.
     */
    suspend fun updateUser(
        userId: String,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        enabled: Boolean? = null,
        emailVerified: Boolean? = null,
    ): UserUpdateResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakAdminService.AdminTokenResult.Success -> adminResult.accessToken
                    is KeycloakAdminService.AdminTokenResult.Error -> return@withTimeout UserUpdateResult.Error(
                        adminResult.errorCode,
                        adminResult.errorMessage
                    )
                }

                // 2. 사용자 정보 수정 요청
                val request = KeycloakUpdateUserRequest(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    enabled = enabled,
                    emailVerified = emailVerified
                )

                when (val response = keycloakApiClient.updateUser(adminToken, userId, request)) {
                    is KeycloakUserResponse -> UserUpdateResult.Success
                    is KeycloakErrorResponse -> UserUpdateResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorMessage ?: response.errorDescription ?: "사용자 정보 수정 실패"
                    )
                    else -> UserUpdateResult.Error("UNKNOWN", "알 수 없는 응답 형식")
                }
            }
        } catch (_: TimeoutCancellationException) {
            UserUpdateResult.Error("TIMEOUT", "사용자 정보 수정 요청 시간 초과")
        } catch (e: Exception) {
            UserUpdateResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    /**
     * Access Token으로 사용자 정보를 조회합니다.
     */
    suspend fun getUserInfo(accessToken: String): UserInfoDetailResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                when (val response = keycloakApiClient.getUserInfo(accessToken)) {
                    is KeycloakUserInfoResponse -> UserInfoDetailResult.Success(response)
                    is KeycloakErrorResponse -> UserInfoDetailResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "사용자 정보 조회 실패"
                    )
                    else -> UserInfoDetailResult.Error("UNKNOWN", "알 수 없는 응답 형식")
                }
            }
        } catch (_: TimeoutCancellationException) {
            UserInfoDetailResult.Error("TIMEOUT", "사용자 정보 조회 요청 시간 초과")
        } catch (e: Exception) {
            UserInfoDetailResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    sealed class UserResult {
        data class Success(val userId: String) : UserResult()
        data class Error(val errorCode: String, val errorMessage: String) : UserResult()
    }

    sealed class UserInfoResult {
        data class Success(val userResponse: KeycloakUserResponse) : UserInfoResult()
        data class Error(val errorCode: String, val errorMessage: String) : UserInfoResult()
    }

    sealed class UserUpdateResult {
        object Success : UserUpdateResult()
        data class Error(val errorCode: String, val errorMessage: String) : UserUpdateResult()
    }

    sealed class UserInfoDetailResult {
        data class Success(val userInfoResponse: KeycloakUserInfoResponse) : UserInfoDetailResult()
        data class Error(val errorCode: String, val errorMessage: String) : UserInfoDetailResult()
    }
}