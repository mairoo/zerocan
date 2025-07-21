package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * UserInfo 데이터
 */
data class KeycloakUserInfoResponse(
    @JsonProperty("sub")
    val sub: String,

    @JsonProperty("email_verified")
    val emailVerified: Boolean,

    @JsonProperty("preferred_username")
    val preferredUsername: String,

    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("given_name")
    val givenName: String? = null,

    @JsonProperty("family_name")
    val familyName: String? = null,

    @JsonProperty("email")
    val email: String? = null,
)