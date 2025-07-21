package kr.pincoin.api.external.auth.keycloak.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "keycloak")
data class KeycloakProperties(
    val realm: String = "zerocan",

    val clientId: String = "zerocan-backend",

    val clientSecret: String = "",

    val serverUrl: String = "http://keycloak:8080",

    val timeout: Long = 10000,
) {
}