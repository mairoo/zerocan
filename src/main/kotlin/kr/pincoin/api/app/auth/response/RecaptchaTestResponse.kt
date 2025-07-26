package kr.pincoin.api.app.auth.response

import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaVerifyData

/**
 * reCAPTCHA 테스트 응답
 */
data class RecaptchaTestResponse(
    val success: Boolean,

    val message: String,

    val data: RecaptchaVerifyData? = null,
)