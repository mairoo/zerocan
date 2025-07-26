package kr.pincoin.api.external.auth.recaptcha.api.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Google reCAPTCHA API 응답
 */
data class RecaptchaVerifyResponse(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("challenge_ts")
    val challengeTs: String? = null,

    @JsonProperty("hostname")
    val hostname: String? = null,

    @JsonProperty("error-codes")
    val errorCodes: List<String>? = null,

    @JsonProperty("score")
    val score: Double? = null, // v3용

    @JsonProperty("action")
    val action: String? = null, // v3용
)