package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * 사용자 생성 데이터: 헤더에서 값 추출
 */
data class KeycloakCreateUserData(
    val userId: String
)