package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * Keycloak 사용자 삭제 응답
 * DELETE 요청은 일반적으로 204 No Content 응답을 반환
 */
data class KeycloakDeleteUserResponse(
    val success: Boolean = true
) : KeycloakResponse