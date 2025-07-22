package kr.pincoin.api.external.notification.mailgun.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.mailgun.api.request.MailgunRequest
import kr.pincoin.api.external.notification.mailgun.api.response.MailgunResponse
import kr.pincoin.api.external.notification.mailgun.error.MailgunErrorCode
import kr.pincoin.api.external.notification.mailgun.properties.MailgunProperties
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class MailgunApiClient(
    private val mailgunWebClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val mailgunProperties: MailgunProperties,
) {
    fun sendEmail(request: MailgunRequest): Mono<MailgunResponse> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("from", mailgunProperties.from)
            add("to", request.to)
            add("subject", request.subject)
            add("text", request.text)
            request.html?.let { add("html", it) }
        }

        return mailgunWebClient
            .post()
            .uri("/v3/${mailgunProperties.domain}/messages")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(String::class.java)
            .flatMap { response ->
                try {
                    val mailgunResponse = objectMapper.readValue(response, MailgunResponse::class.java)
                    Mono.just(mailgunResponse)
                } catch (_: Exception) {
                    Mono.error(BusinessException(MailgunErrorCode.MAILGUN_API_PARSE_ERROR))
                }
            }
            .onErrorMap { e ->
                when (e) {
                    is BusinessException -> e
                    else -> BusinessException(MailgunErrorCode.MAILGUN_API_SEND_ERROR)
                }
            }
    }
}