package kr.pincoin.api.external.auth.keycloak.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.nio.charset.StandardCharsets
import java.time.Duration

@Configuration
class KeycloakWebClientConfig(
    private val objectMapper: ObjectMapper,
    private val keycloakProperties: KeycloakProperties,
) {
    @Bean
    fun keycloakWebClient(): WebClient =
        WebClient.builder()
            .baseUrl(keycloakProperties.serverUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                // 응답 크기 제한 설정 (기본 256KB → 1MB)
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(10))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                )
            )
            .build()
}