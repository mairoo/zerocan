package kr.pincoin.api.app.user.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kr.pincoin.api.domain.user.model.User
import kr.pincoin.api.domain.user.model.enums.Role
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("created")
    val created: LocalDateTime,

    @JsonProperty("modified")
    val modified: LocalDateTime,

    @JsonProperty("keycloakId")
    val keycloakId: String,

    @JsonProperty("isActive")
    val isActive: Boolean,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("roles")
    val roles: List<Role>,
) {
    companion object {
        fun from(user: User) = with(user) {
            UserResponse(
                id = id ?: throw kotlin.IllegalStateException("사용자 ID는 필수 입력값입니다"),
                created = created ?: LocalDateTime.now(),
                modified = modified ?: LocalDateTime.now(),
                keycloakId = keycloakId,
                isActive = isActive,
                name = name,
                email = email,
                roles = roles,
            )
        }
    }
}