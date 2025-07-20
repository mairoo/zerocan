package kr.pincoin.api.external.auth.keycloak.service

import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class KeycloakAuthService(
    private val keycloakWebClient: WebClient,
    private val keycloakProperties: KeycloakProperties,
) {

}