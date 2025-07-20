package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

// 토큰 응답 (로그인, 토큰 갱신, Admin 토큰)
data class KeycloakTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,

    @JsonProperty("refresh_expires_in")
    val refreshExpiresIn: Long,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("not_before_policy")
    val notBeforePolicy: Long? = null,

    @JsonProperty("session_state")
    val sessionState: String? = null,

    @JsonProperty("scope")
    val scope: String? = null
) : KeycloakResponse