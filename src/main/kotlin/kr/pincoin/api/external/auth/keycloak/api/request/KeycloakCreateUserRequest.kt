package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

// Admin API - 사용자 생성
data class KeycloakCreateUserRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @JsonProperty("username")
    val username: String,

    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "이름은 필수입니다")
    @JsonProperty("firstName")
    val firstName: String,

    @field:NotBlank(message = "성은 필수입니다")
    @JsonProperty("lastName")
    val lastName: String,

    @JsonProperty("enabled")
    val enabled: Boolean = true,

    @JsonProperty("emailVerified")
    val emailVerified: Boolean = false,

    @JsonProperty("credentials")
    val credentials: List<KeycloakCredential>? = null
) {
    data class KeycloakCredential(
        @JsonProperty("type")
        val type: String = "password",

        @JsonProperty("value")
        val value: String,

        @JsonProperty("temporary")
        val temporary: Boolean = false
    )
}
