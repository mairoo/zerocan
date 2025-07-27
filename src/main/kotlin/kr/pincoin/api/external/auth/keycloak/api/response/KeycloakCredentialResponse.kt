package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 사용자 인증정보 조회 응답
 */
data class KeycloakCredentialResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("userLabel")
    val userLabel: String? = null,

    @JsonProperty("createdDate")
    val createdDate: Long? = null,

    @JsonProperty("secretData")
    val secretData: String? = null,

    @JsonProperty("credentialData")
    val credentialData: String? = null,
)