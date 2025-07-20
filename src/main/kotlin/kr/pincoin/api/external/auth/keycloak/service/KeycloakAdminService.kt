package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakAdminTokenRequest
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakErrorResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakTokenResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakAdminService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakProperties: KeycloakProperties,
) {
    /**
     * Admin 토큰을 획득합니다.
     */
    suspend fun getAdminToken(): AdminTokenResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(10000) { // 10초
                val request = KeycloakAdminTokenRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret
                )

                when (val response = keycloakApiClient.getAdminToken(request)) {
                    is KeycloakTokenResponse -> AdminTokenResult.Success(response.accessToken)
                    is KeycloakErrorResponse -> AdminTokenResult.Error(
                        errorCode = response.error,
                        errorMessage = response.errorDescription ?: "Admin 토큰 획득 실패"
                    )
                    else -> AdminTokenResult.Error(
                        errorCode = "UNKNOWN",
                        errorMessage = "알 수 없는 응답 형식"
                    )
                }
            }
        } catch (_: TimeoutCancellationException) {
            AdminTokenResult.Error("TIMEOUT", "Admin 토큰 요청 시간 초과")
        } catch (e: Exception) {
            AdminTokenResult.Error("SYSTEM_ERROR", e.message ?: "시스템 오류")
        }
    }

    sealed class AdminTokenResult {
        data class Success(val accessToken: String) : AdminTokenResult()
        data class Error(val errorCode: String, val errorMessage: String) : AdminTokenResult()
    }
}