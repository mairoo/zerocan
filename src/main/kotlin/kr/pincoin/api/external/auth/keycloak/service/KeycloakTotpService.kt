package kr.pincoin.api.external.auth.keycloak.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakLogoutResponse
import kr.pincoin.api.external.auth.keycloak.api.response.KeycloakResponse
import kr.pincoin.api.external.auth.keycloak.api.response.TotpSetupData
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.*

@Service
class KeycloakTotpService(
    private val keycloakApiClient: KeycloakApiClient,
    private val keycloakAdminService: KeycloakAdminService,
    private val keycloakProperties: KeycloakProperties,
) {
    /**
     * 사용자에게 TOTP 설정 강제 (필수 액션 추가)
     * 다음 로그인 시 Keycloak에서 2FA 설정 화면을 표시합니다.
     */
    suspend fun addTotpRequiredAction(userId: String): KeycloakResponse<KeycloakLogoutResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(keycloakProperties.timeout) {
                    val adminToken = getAdminToken()
                        ?: return@withTimeout KeycloakResponse.Error("ADMIN_TOKEN_FAILED", "관리자 토큰 획득 실패")

                    keycloakApiClient.setUserRequiredActions(adminToken, userId, listOf("CONFIGURE_TOTP"))
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("TOTP 필수 액션 추가")
            } catch (e: Exception) {
                handleError(e, "TOTP 필수 액션 추가")
            }
        }

    /**
     * TOTP Secret 생성 및 설정 데이터 반환
     * 실제 Keycloak에는 저장하지 않고 클라이언트가 사용할 데이터만 반환합니다.
     */
    fun generateTotpSetupData(userId: String): TotpSetupData {
        val secret = generateBase32Secret()
        return TotpSetupData.create(userId, secret)
    }

    /**
     * TOTP 인증정보를 Keycloak에 저장
     * OTP 코드 검증은 호출자가 미리 수행했다고 가정합니다.
     */
    suspend fun saveTotpCredential(
        userId: String,
        secret: String
    ): KeycloakResponse<KeycloakLogoutResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(keycloakProperties.timeout) {
                    val adminToken = getAdminToken()
                        ?: return@withTimeout KeycloakResponse.Error("ADMIN_TOKEN_FAILED", "관리자 토큰 획득 실패")

                    keycloakApiClient.createTotpCredential(adminToken, userId, secret)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("TOTP 인증정보 저장")
            } catch (e: Exception) {
                handleError(e, "TOTP 인증정보 저장")
            }
        }

    /**
     * 사용자의 TOTP 활성화 상태 확인
     */
    suspend fun isUserTotpEnabled(userId: String): KeycloakResponse<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(keycloakProperties.timeout) {
                    val adminToken = getAdminToken()
                        ?: return@withTimeout KeycloakResponse.Error("ADMIN_TOKEN_FAILED", "관리자 토큰 획득 실패")

                    when (val credentialsResult = keycloakApiClient.getUserCredentials(adminToken, userId)) {
                        is KeycloakResponse.Success -> {
                            val hasTotpCredential = credentialsResult.data.any { it.type == "otp" }
                            KeycloakResponse.Success(hasTotpCredential)
                        }

                        is KeycloakResponse.Error -> {
                            KeycloakResponse.Error<Boolean>(
                                errorCode = credentialsResult.errorCode,
                                errorMessage = credentialsResult.errorMessage
                            )
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("TOTP 상태 확인")
            } catch (e: Exception) {
                handleError(e, "TOTP 상태 확인")
            }
        }

    /**
     * 사용자의 TOTP 인증정보 삭제
     */
    suspend fun deleteTotpCredential(userId: String): KeycloakResponse<KeycloakLogoutResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(keycloakProperties.timeout) {
                    val adminToken = getAdminToken()
                        ?: return@withTimeout KeycloakResponse.Error("ADMIN_TOKEN_FAILED", "관리자 토큰 획득 실패")

                    // 1. 사용자의 TOTP 인증정보 조회
                    when (val credentialsResult = keycloakApiClient.getUserCredentials(adminToken, userId)) {
                        is KeycloakResponse.Success -> {
                            val totpCredential = credentialsResult.data.find { it.type == "otp" }

                            if (totpCredential != null) {
                                // 2. TOTP 인증정보 삭제
                                keycloakApiClient.deleteCredential(adminToken, userId, totpCredential.id)
                            } else {
                                KeycloakResponse.Success(KeycloakLogoutResponse) // 이미 삭제됨
                            }
                        }

                        is KeycloakResponse.Error -> {
                            KeycloakResponse.Error<KeycloakLogoutResponse>(
                                errorCode = credentialsResult.errorCode,
                                errorMessage = credentialsResult.errorMessage
                            )
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("TOTP 인증정보 삭제")
            } catch (e: Exception) {
                handleError(e, "TOTP 인증정보 삭제")
            }
        }

    private suspend fun getAdminToken(): String? {
        return when (val adminResult = keycloakAdminService.getAdminToken()) {
            is KeycloakResponse.Success -> adminResult.data.accessToken
            is KeycloakResponse.Error -> null
        }
    }

    /**
     * Base32 Secret 생성
     */
    private fun generateBase32Secret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20) // 160 bits
        random.nextBytes(bytes)

        // Base32 인코딩 (실제로는 Apache Commons Codec 사용 권장)
        return Base64.getEncoder().encodeToString(bytes)
            .replace("=", "")
            .replace("+", "A")
            .replace("/", "B")
            .take(32) // 32자리로 제한
    }

    private fun handleTimeout(operation: String): KeycloakResponse.Error<Nothing> =
        KeycloakResponse.Error("TIMEOUT", "$operation 요청 시간 초과")

    private fun handleError(error: Throwable, operation: String): KeycloakResponse.Error<Nothing> =
        KeycloakResponse.Error("SYSTEM_ERROR", "${operation} 중 오류 발생: ${error.message ?: "알 수 없는 오류"}")
}