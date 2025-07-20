package kr.pincoin.api.external.auth.keycloak.api.response

import com.fasterxml.jackson.annotation.JsonProperty

// 에러 응답
data class KeycloakErrorResponse(
    @JsonProperty("error")
    val error: String,

    @JsonProperty("error_description")
    val errorDescription: String? = null,

    @JsonProperty("errorMessage")
    val errorMessage: String? = null
) : KeycloakResponse