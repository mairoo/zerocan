package kr.pincoin.api.external.notification.mailgun.config

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.mailgun.properties.MailgunProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets

@Configuration
class MailgunWebClientConfig(
    private val objectMapper: ObjectMapper,
    private val mailgunProperties: MailgunProperties,
) {
    @Bean
    fun mailgunWebClient(): WebClient =
        WebClient.builder()
            .baseUrl(mailgunProperties.baseUrl)
            .defaultHeaders { headers ->
                headers.setBasicAuth("api", mailgunProperties.key)
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