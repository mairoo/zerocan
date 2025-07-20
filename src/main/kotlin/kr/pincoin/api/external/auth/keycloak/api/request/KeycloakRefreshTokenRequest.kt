package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

// 토큰 갱신
data class KeycloakRefreshTokenRequest(
    @field:NotBlank(message = "클라이언트 ID는 필수입니다")
    @JsonProperty("client_id")
    val clientId: String,

    @field:NotBlank(message = "클라이언트 시크릿은 필수입니다")
    @JsonProperty("client_secret")
    val clientSecret: String,

    @JsonProperty("grant_type")
    val grantType: String = "refresh_token",

    @field:NotBlank(message = "리프레시 토큰은 필수입니다")
    @JsonProperty("refresh_token")
    val refreshToken: String
)