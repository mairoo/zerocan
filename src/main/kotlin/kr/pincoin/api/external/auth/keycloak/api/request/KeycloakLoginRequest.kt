package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

// Direct Grant - 로그인
data class KeycloakLoginRequest(
    @field:NotBlank(message = "클라이언트 ID는 필수입니다")
    @JsonProperty("client_id")
    val clientId: String,

    @field:NotBlank(message = "클라이언트 시크릿은 필수입니다")
    @JsonProperty("client_secret")
    val clientSecret: String,

    @JsonProperty("grant_type")
    val grantType: String = "password",

    @field:NotBlank(message = "사용자명은 필수입니다")
    @JsonProperty("username")
    val username: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @JsonProperty("password")
    val password: String,

    @JsonProperty("scope")
    val scope: String = "openid profile email",

    @JsonProperty("totp")
    val totp: String? = null,
)