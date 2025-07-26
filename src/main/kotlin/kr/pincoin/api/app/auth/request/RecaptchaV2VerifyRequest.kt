package kr.pincoin.api.app.auth.request

import jakarta.validation.constraints.NotBlank

/**
 * reCAPTCHA v2 검증 요청
 */
data class RecaptchaV2VerifyRequest(
    @field:NotBlank(message = "reCAPTCHA 토큰은 필수입니다")
    val token: String
)