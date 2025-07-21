package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * UserInfo 데이터
 */
data class KeycloakUserInfoData(
    val sub: String,
    val emailVerified: Boolean,
    val preferredUsername: String,
    val name: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val email: String? = null,
)