package kr.pincoin.api.external.auth.keycloak.api.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 사용자에게 필수 액션 설정 - CONFIGURE_TOTP 강제
 */
data class KeycloakRequiredActionRequest(
    @JsonProperty("requiredActions")
    val requiredActions: List<String>,
)
