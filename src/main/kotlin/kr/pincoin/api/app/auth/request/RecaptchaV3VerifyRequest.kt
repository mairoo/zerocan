package kr.pincoin.api.app.auth.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank

/**
 * reCAPTCHA v3 검증 요청
 */
data class RecaptchaV3VerifyRequest(
    @field:NotBlank(message = "reCAPTCHA 토큰은 필수입니다")
    val token: String,

    @field:DecimalMin(value = "0.0", message = "최소 점수는 0.0 이상이어야 합니다")
    @field:DecimalMax(value = "1.0", message = "최소 점수는 1.0 이하여야 합니다")
    val minScore: Double? = null,
)
