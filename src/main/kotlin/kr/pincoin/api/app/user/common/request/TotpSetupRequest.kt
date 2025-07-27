package kr.pincoin.api.app.user.common.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * TOTP 설정 완료 요청
 *
 * 사용자가 Google Authenticator 등에서 생성된 6자리 OTP 코드를 입력하여
 * 2FA 설정을 완료할 때 사용됩니다.
 */
data class TotpSetupRequest(
    /**
     * Google Authenticator 등에서 생성된 6자리 OTP 코드
     *
     * TOTP Secret을 기반으로 현재 시간에 생성된 6자리 숫자입니다.
     * 이 코드를 통해 사용자가 올바른 Secret을 가지고 있는지 검증합니다.
     */
    @field:NotBlank(message = "OTP 코드는 필수입니다")
    @field:Pattern(
        regexp = "^[0-9]{6}$",
        message = "OTP 코드는 6자리 숫자여야 합니다"
    )
    val otpCode: String
)