package kr.pincoin.api.external.notification.aligo.config

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.aligo.properties.AligoProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets

@Configuration
class AligoWebClientConfig(
    private val objectMapper: ObjectMapper,
    private val aligoProperties: AligoProperties,
) {
    @Bean
    fun aligoWebClient(): WebClient =
        WebClient.builder()
            .baseUrl(aligoProperties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()
}