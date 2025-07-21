package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakCreateUserRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakUpdateUserRequest
import kr.pincoin.api.external.auth.keycloak.api.response.*
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakUserService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakAdminService: KeycloakAdminService,
    private val keycloakProperties: KeycloakProperties,
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
    ): KeycloakResponse<KeycloakCreateUserData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakResponse.Success -> adminResult.data.accessToken
                    is KeycloakResponse.Error -> return@withTimeout KeycloakResponse.Error(
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

                keycloakApiClient.createUser(adminToken, request)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("사용자 생성")
        } catch (e: Exception) {
            handleError(e, "사용자 생성")
        }
    }

    /**
     * 사용자 정보를 조회합니다.
     */
    suspend fun getUser(userId: String): KeycloakResponse<KeycloakUserData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakResponse.Success -> adminResult.data.accessToken
                    is KeycloakResponse.Error -> return@withTimeout KeycloakResponse.Error(
                        adminResult.errorCode,
                        adminResult.errorMessage
                    )
                }

                // 2. 사용자 정보 조회
                keycloakApiClient.getUser(adminToken, userId)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("사용자 정보 조회")
        } catch (e: Exception) {
            handleError(e, "사용자 정보 조회")
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
    ): KeycloakResponse<KeycloakUserData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakResponse.Success -> adminResult.data.accessToken
                    is KeycloakResponse.Error -> return@withTimeout KeycloakResponse.Error(
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

                keycloakApiClient.updateUser(adminToken, userId, request)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("사용자 정보 수정")
        } catch (e: Exception) {
            handleError(e, "사용자 정보 수정")
        }
    }

    /**
     * 사용자를 삭제합니다.
     */
    suspend fun deleteUser(userId: String): KeycloakResponse<KeycloakLogoutData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                // 1. Admin 토큰 획득
                val adminToken = when (val adminResult = keycloakAdminService.getAdminToken()) {
                    is KeycloakResponse.Success -> adminResult.data.accessToken
                    is KeycloakResponse.Error -> return@withTimeout KeycloakResponse.Error(
                        adminResult.errorCode,
                        adminResult.errorMessage
                    )
                }

                // 2. 사용자 삭제
                keycloakApiClient.deleteUser(adminToken, userId)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("사용자 삭제")
        } catch (e: Exception) {
            handleError(e, "사용자 삭제")
        }
    }

    /**
     * Access Token으로 사용자 정보를 조회합니다.
     */
    suspend fun getUserInfo(accessToken: String): KeycloakResponse<KeycloakUserInfoData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                keycloakApiClient.getUserInfo(accessToken)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("사용자 정보 조회")
        } catch (e: Exception) {
            handleError(e, "사용자 정보 조회")
        }
    }

    // Transfer Services 패턴을 따른 공통 에러 처리 메서드들
    private fun handleTimeout(operation: String): KeycloakResponse<Nothing> {
        return KeycloakResponse.Error(
            errorCode = "TIMEOUT",
            errorMessage = "$operation 요청 시간 초과"
        )
    }

    private fun handleError(error: Throwable, operation: String): KeycloakResponse<Nothing> {
        return KeycloakResponse.Error(
            errorCode = "SYSTEM_ERROR",
            errorMessage = "${operation} 중 오류 발생: ${error.message ?: "알 수 없는 오류"}"
        )
    }
}