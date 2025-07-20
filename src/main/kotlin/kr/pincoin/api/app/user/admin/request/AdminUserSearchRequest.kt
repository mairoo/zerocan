package kr.pincoin.api.app.user.admin.request

import com.fasterxml.jackson.annotation.JsonProperty
import kr.pincoin.api.domain.user.model.enums.Role

data class AdminUserSearchRequest(
    @JsonProperty("userIsActive")
    val userIsActive: Boolean? = null,

    @JsonProperty("role")
    val role: Role? = null,

    @JsonProperty("name")
    val name: String? = null,

    @JsonProperty("email")
    val email: String? = null,
)