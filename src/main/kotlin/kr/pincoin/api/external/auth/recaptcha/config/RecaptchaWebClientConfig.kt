package kr.pincoin.api.external.auth.recaptcha.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import kr.pincoin.api.external.auth.recaptcha.properties.RecaptchaProperties
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
class RecaptchaWebClientConfig(
    private val objectMapper: ObjectMapper,
    private val recaptchaProperties: RecaptchaProperties,
) {
    @Bean
    fun recaptchaWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("https://www.google.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
            .defaultHeader(HttpHeaders.USER_AGENT, "reCAPTCHA-Client/1.0")
            .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
                configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                // 응답 크기 제한 설정 (reCAPTCHA 응답은 작으므로 256KB면 충분)
                configurer.defaultCodecs().maxInMemorySize(256 * 1024)
            }
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .responseTimeout(Duration.ofMillis(recaptchaProperties.timeout))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, recaptchaProperties.timeout.toInt())
                )
            )
            .build()
}