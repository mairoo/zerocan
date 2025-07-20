package kr.pincoin.api.app.auth.service

import kr.pincoin.api.global.config.KeycloakWebClientConfig
import kr.pincoin.api.global.properties.KeycloakProperties
import org.springframework.stereotype.Service

@Service
class KeycloakAuthService(
    private val keycloakWebClient: KeycloakWebClientConfig,
    private val keycloakProperties: KeycloakProperties,
) {

}