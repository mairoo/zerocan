package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLoginRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakLogoutRequest
import kr.pincoin.api.external.auth.keycloak.api.request.KeycloakRefreshTokenRequest
import kr.pincoin.api.external.auth.keycloak.api.response.*
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakTokenService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakProperties: KeycloakProperties,
) {
    /**
     * 사용자 로그인을 수행합니다.
     */
    suspend fun login(
        username: String,
        password: String,
    ): KeycloakResponse<KeycloakTokenData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                val request = KeycloakLoginRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    username = username,
                    password = password
                )

                keycloakApiClient.login(request)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("로그인")
        } catch (e: Exception) {
            handleError(e, "로그인")
        }
    }

    /**
     * 토큰을 갱신합니다.
     */
    suspend fun refreshToken(
        refreshToken: String
    ): KeycloakResponse<KeycloakTokenData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                val request = KeycloakRefreshTokenRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    refreshToken = refreshToken
                )

                keycloakApiClient.refreshToken(request)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("토큰 갱신")
        } catch (e: Exception) {
            handleError(e, "토큰 갱신")
        }
    }

    /**
     * 사용자를 로그아웃합니다.
     */
    suspend fun logout(
        refreshToken: String
    ): KeycloakResponse<KeycloakLogoutData> = withContext(Dispatchers.IO) {
        try {
            withTimeout(keycloakProperties.timeout) {
                val request = KeycloakLogoutRequest(
                    clientId = keycloakProperties.clientId,
                    clientSecret = keycloakProperties.clientSecret,
                    refreshToken = refreshToken
                )

                keycloakApiClient.logout(request)
            }
        } catch (_: TimeoutCancellationException) {
            handleTimeout("로그아웃")
        } catch (e: Exception) {
            handleError(e, "로그아웃")
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