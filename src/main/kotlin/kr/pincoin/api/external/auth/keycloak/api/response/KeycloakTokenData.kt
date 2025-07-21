package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * 토큰 관련 데이터
 */
data class KeycloakTokenData(
    val accessToken: String,
    val expiresIn: Long,
    val refreshExpiresIn: Long,
    val refreshToken: String? = null,
    val tokenType: String,
    val idToken: String? = null,
    val sessionState: String? = null,
    val scope: String? = null,
)