package kr.pincoin.api.external.auth.keycloak.api.response

// 사용자 생성 응답 (Location 헤더에서 ID 추출)
data class KeycloakCreateUserResponse(
    val userId: String
) : KeycloakResponse