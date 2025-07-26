package kr.pincoin.api.app.auth.response

/**
 * reCAPTCHA 상태 응답
 */
data class RecaptchaStatusResponse(
    val enabled: Boolean,
    val message: String,
)