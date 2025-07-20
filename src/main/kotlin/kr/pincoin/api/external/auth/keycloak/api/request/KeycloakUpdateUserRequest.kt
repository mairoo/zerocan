package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email

// 사용자 정보 수정
data class KeycloakUpdateUserRequest(
    @JsonProperty("firstName")
    val firstName: String? = null,

    @JsonProperty("lastName")
    val lastName: String? = null,

    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @JsonProperty("email")
    val email: String? = null,

    @JsonProperty("enabled")
    val enabled: Boolean? = null,

    @JsonProperty("emailVerified")
    val emailVerified: Boolean? = null
)