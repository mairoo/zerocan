package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * TOTP 설정 요청
 */
data class KeycloakTotpSetupRequest(
    @JsonProperty("type")
    val type: String = "totp",

    @JsonProperty("algorithm")
    val algorithm: String = "HmacSHA1",

    @JsonProperty("digits")
    val digits: Int = 6,

    @JsonProperty("period")
    val period: Int = 30,

    @JsonProperty("secretData")
    val secretData: String, // JSON 문자열: {"value":"BASE32_SECRET"}

    @JsonProperty("credentialData")
    val credentialData: String, // JSON 문자열: {"subType":"totp","digits":6,"period":30,"algorithm":"HmacSHA1"}
)