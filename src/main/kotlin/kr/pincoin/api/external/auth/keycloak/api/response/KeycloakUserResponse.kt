package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 사용자 정보 데이터
 */
data class KeycloakUserResponse(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("username")
    val username: String,

    @JsonProperty("enabled")
    val enabled: Boolean,

    @JsonProperty("emailVerified")
    val emailVerified: Boolean,

    @JsonProperty("firstName")
    val firstName: String? = null,

    @JsonProperty("lastName")
    val lastName: String? = null,

    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("createdTimestamp")
    val createdTimestamp: Long? = null,
)