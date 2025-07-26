package kr.pincoin.api.external.auth.recaptcha.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "recaptcha")
data class RecaptchaProperties(
    val siteKey: String = "",
    val secretKey: String = "",
    val verifyUrl: String = "https://www.google.com/recaptcha/api/siteverify",
    val timeout: Long = 5000,
    val enabled: Boolean = true,
    val minScore: Double = 0.5, // v3용 최소 점수
)
