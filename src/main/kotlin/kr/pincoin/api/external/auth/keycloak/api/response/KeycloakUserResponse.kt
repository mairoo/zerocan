package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

// 사용자 정보 응답
data class KeycloakUserResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("createdTimestamp")
    val createdTimestamp: Long? = null,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("enabled")
    val enabled: Boolean,

    @JsonProperty("totp")
    val totp: Boolean,

    @JsonProperty("emailVerified")
    val emailVerified: Boolean,

    @JsonProperty("firstName")
    val firstName: String? = null,

    @JsonProperty("lastName")
    val lastName: String? = null,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("attributes")
    val attributes: Map<String, List<String>>? = null,

    @JsonProperty("disableableCredentialTypes")
    val disableableCredentialTypes: List<String>? = null,

    @JsonProperty("requiredActions")
    val requiredActions: List<String>? = null,

    @JsonProperty("notBefore")
    val notBefore: Long? = null,

    @JsonProperty("access")
    val access: Map<String, Boolean>? = null
) : KeycloakResponse