package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * 사용자 정보 데이터
 */
data class KeycloakUserData(
    val id: String,
    val username: String,
    val enabled: Boolean,
    val emailVerified: Boolean,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val createdTimestamp: Long? = null,
)