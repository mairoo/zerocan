package kr.pincoin.api.external.auth.recaptcha.api.request

import jakarta.validation.constraints.NotBlank

data class RecaptchaVerifyRequest(
    @field:NotBlank(message = "reCAPTCHA 토큰은 필수입니다")
    val token: String,
)