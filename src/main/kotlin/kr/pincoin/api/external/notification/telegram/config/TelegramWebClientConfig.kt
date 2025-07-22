package kr.pincoin.api.external.notification.telegram.config

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.telegram.properties.TelegramProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets

@Configuration
class TelegramWebClientConfig(
    private val objectMapper: ObjectMapper,
    private val telegramProperties: TelegramProperties,
) {
    @Bean
    fun telegramWebClient(): WebClient =
        WebClient.builder()
            .baseUrl(telegramProperties.baseUrl)
            .defaultHeaders { headers ->
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                headers.set(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
                headers.set(HttpHeaders.CACHE_CONTROL, "no-cache")
            }
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
            }
            .build()
}