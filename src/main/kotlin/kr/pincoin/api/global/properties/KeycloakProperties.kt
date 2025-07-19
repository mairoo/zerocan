package kr.pincoin.api.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keycloak")
data class KeycloakProperties(
    val enabled: Boolean = false,

    val userMigration: UserMigration = UserMigration(),

    val realm: String = "zerocan",

    val clientId: String = "zerocan-backend",

    val clientSecret: String = "",

    val serverUrl: String = "http://keycloak:8080"
) {

    data class UserMigration(
        val autoCreate: Boolean = false,
    )
}