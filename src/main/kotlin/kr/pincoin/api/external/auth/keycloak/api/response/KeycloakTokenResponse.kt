package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonAlias
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
    val refreshToken: String? = null, // Admin 토큰에는 없을 수 있음

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("id_token")
    val idToken: String? = null,

    @JsonProperty("not_before_policy")
    @JsonAlias("not-before-policy")
    val notBeforePolicy: Long? = null,

    @JsonProperty("session_state")
    val sessionState: String? = null,

    @JsonProperty("scope")
    val scope: String? = null,
) : KeycloakResponse