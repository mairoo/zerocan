package kr.pincoin.api.app.auth.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignInRequest(
    @field:NotBlank(message = "이메일은 필수 입력값입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:Size(max = 32, message = "이메일은 32자를 초과할 수 없습니다")
    @JsonProperty("email")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수 입력값입니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,30}$",
        message = "비밀번호는 8~30자리이면서 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    @JsonProperty("password")
    val password: String,

    @JsonProperty("rememberMe")
    val rememberMe: Boolean = false
)