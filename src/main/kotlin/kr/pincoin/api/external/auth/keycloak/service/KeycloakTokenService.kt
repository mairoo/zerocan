package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLoginRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLogoutRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakRefreshTokenRequest
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakErrorResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakLogoutResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakTokenResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakTokenService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakProperties: KeycloakProperties,
) {
    /**
     * 사용자 로그인을 수행합니다.
     *
     * @param username 사용자명 또는 이메일
     * @param password 비밀번호
     * @return 로그인 결과 (성공 시 토큰 정보, 실패 시 에러 정보)
     */
    suspend fun login(
        username: String,
        password: String,
    ): TokenResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                val request = KeycloakLoginRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    username = username,
                    password = password
                )

                when (val response = keycloakApiClient.login(request)) {
                    is KeycloakTokenResponse -> TokenResult.Success(response)
                    is KeycloakErrorResponse -> TokenResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "로그인 실패"
                    )

                    else -> TokenResult.Error(
                        errorCode = "UNKNOWN",
                        errorMessage = "알 수 없는 응답 형식"
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            TokenResult.Error(
                errorCode = "TIMEOUT",
                errorMessage = "로그인 요청 시간 초과"
            )
        } catch (e: Exception) {
            TokenResult.Error(
                errorCode = "SYSTEM_ERROR",
                errorMessage = e.message ?: "시스템 오류"
            )
        }
    }

    /**
     * 토큰을 갱신합니다.
     */
    suspend fun refreshToken(refreshToken: String): TokenResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                val request = KeycloakRefreshTokenRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    refreshToken = refreshToken
                )

                when (val response = keycloakApiClient.refreshToken(request)) {
                    is KeycloakTokenResponse -> TokenResult.Success(response)
                    is KeycloakErrorResponse -> TokenResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "토큰 갱신 실패"
                    )
                    else -> TokenResult.Error(
                        errorCode = "UNKNOWN",
                        errorMessage = "알 수 없는 응답 형식"
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            TokenResult.Error("TIMEOUT", "토큰 갱신 요청 시간 초과")
        } catch (e: Exception) {
            TokenResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    /**
     * 사용자를 로그아웃합니다.
     */
    suspend fun logout(refreshToken: String): LogoutResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                val request = KeycloakLogoutRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    refreshToken = refreshToken
                )

                when (val response = keycloakApiClient.logout(request)) {
                    is KeycloakLogoutResponse -> LogoutResult.Success
                    is KeycloakErrorResponse -> LogoutResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "로그아웃 실패"
                    )
                    else -> LogoutResult.Error(
                        errorCode = "UNKNOWN",
                        errorMessage = "알 수 없는 응답 형식"
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            LogoutResult.Error("TIMEOUT", "로그아웃 요청 시간 초과")
        } catch (e: Exception) {
            LogoutResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    sealed class TokenResult {
        data class Success(val tokenResponse: KeycloakTokenResponse) : TokenResult()
        data class Error(val errorCode: String, val errorMessage: String) : TokenResult()
    }

    sealed class LogoutResult {
        object Success : LogoutResult()
        data class Error(val errorCode: String, val errorMessage: String) : LogoutResult()
    }
}