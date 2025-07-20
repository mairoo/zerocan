package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Keycloak Admin API - 사용자 생성 요청
 *
 * 필수 필드: username만 필수
 * 선택 필드: 나머지는 모두 선택사항
 */
data class KeycloakCreateUserRequest(
    // 필수 필드
    @field:NotBlank(message = "사용자명은 필수입니다")
    @JsonProperty("username")
    val username: String,

    // 선택 필드
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    @JsonProperty("email")
    val email: String,

    @JsonProperty("firstName")
    val firstName: String,

    @JsonProperty("lastName")
    val lastName: String? = null,

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

    companion object {
        /**
         * 일반적인 회원가입 시나리오용
         */
        fun forSignUp(
            username: String,
            email: String,
            firstName: String,
            password: String
        ) = KeycloakCreateUserRequest(
            username = username,
            email = email,
            firstName = firstName,
            lastName = "", // 빈 문자열로 400 에러 방지
            enabled = true,
            emailVerified = false,
            credentials = listOf(
                KeycloakCredential(
                    type = "password",
                    value = password,
                    temporary = false,
                )
            )
        )
    }
}