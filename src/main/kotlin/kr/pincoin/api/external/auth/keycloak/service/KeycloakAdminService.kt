package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakAdminTokenRequest
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakAdminTokenData
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakResponse
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakAdminService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakProperties: KeycloakProperties,
) {
    /**
     * Admin 토큰을 획득
     */
    suspend fun getAdminToken(): KeycloakResponse<KeycloakAdminTokenData> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(keycloakProperties.timeout) {
                    val request = KeycloakAdminTokenRequest(
                        clientId = keycloakProperties.clientId,
                        clientSecret = keycloakProperties.clientSecret
                    )

                    keycloakApiClient.getAdminToken(request)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("Admin 토큰 획득")
            } catch (e: Exception) {
                handleError(e, "Admin 토큰 획득")
            }
        }

    private fun handleTimeout(
        operation: String,
    ): KeycloakResponse<Nothing> =
        KeycloakResponse.Error(
            errorCode = "TIMEOUT",
            errorMessage = "$operation 요청 시간 초과"
        )

    private fun handleError(
        error: Throwable,
        operation: String,
    ): KeycloakResponse<Nothing> =
        KeycloakResponse.Error(
            errorCode = "SYSTEM_ERROR",
            errorMessage = "$operation 중 오류 발생: ${error.message ?: "알 수 없는 오류"}"
        )
}