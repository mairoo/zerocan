package kr.pincoin.api.external.auth.keycloak.api.response

// 사용자 목록 응답 (Admin API)
data class KeycloakUsersResponse(
    val users: List<KeycloakUserResponse>
) : KeycloakResponse