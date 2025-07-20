package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLoginRequest
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakErrorResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakTokenResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakLoginService(
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
    ): LoginResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                val request = KeycloakLoginRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    username = username,
                    password = password
                )

                when (val response = keycloakApiClient.login(request)) {
                    is KeycloakTokenResponse -> LoginResult.Success(response)
                    is KeycloakErrorResponse -> LoginResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "로그인 실패"
                    )

                    else -> LoginResult.Error(
                        errorCode = "UNKNOWN",
                        errorMessage = "알 수 없는 응답 형식"
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            LoginResult.Error(
                errorCode = "TIMEOUT",
                errorMessage = "로그인 요청 시간 초과"
            )
        } catch (e: Exception) {
            LoginResult.Error(
                errorCode = "SYSTEM_ERROR",
                errorMessage = e.message ?: "시스템 오류"
            )
        }
    }

    sealed class LoginResult {
        data class Success(val tokenResponse: KeycloakTokenResponse) : LoginResult()
        data class Error(val errorCode: String, val errorMessage: String) : LoginResult()
    }
}