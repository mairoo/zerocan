package kr.pincoin.api.external.auth.recaptcha.api.response

/**
 * 검증 결과 데이터
 */
data class RecaptchaVerifyData(
    val success: Boolean,
    val score: Double? = null,
    val action: String? = null,
    val hostname: String? = null,
    val challengeTs: String? = null,
    val errorCodes: List<String>? = null,
)
