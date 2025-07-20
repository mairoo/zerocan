package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

// Admin Access Token 획득
data class KeycloakAdminTokenRequest(
    @field:NotBlank(message = "클라이언트 ID는 필수입니다")
    @JsonProperty("client_id")
    val clientId: String,

    @field:NotBlank(message = "클라이언트 시크릿은 필수입니다")
    @JsonProperty("client_secret")
    val clientSecret: String,

    @JsonProperty("grant_type")
    val grantType: String = "client_credentials"
)