package kr.pincoin.api.external.notification.aligo.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.aligo.api.request.AligoSmsRequest
import kr.pincoin.api.external.notification.aligo.api.response.AligoSmsResponse
import kr.pincoin.api.external.notification.aligo.error.AligoErrorCode
import kr.pincoin.api.external.notification.aligo.properties.AligoProperties
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono


@Component
class AligoApiClient(
    private val aligoWebClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val aligoProperties: AligoProperties,
) {
    fun sendSms(request: AligoSmsRequest): Mono<AligoSmsResponse> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("receiver", request.receiver)
            add("msg", request.message)
            add("key", aligoProperties.key)
            add("user_id", aligoProperties.userId)
            add("sender", aligoProperties.sender)
        }

        return aligoWebClient
            .post()
            .uri("/send/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(String::class.java)
            .flatMap { response ->
                try {
                    val aligoResponse = objectMapper.readValue(response, AligoSmsResponse::class.java)
                    Mono.just(aligoResponse)
                } catch (_: Exception) {
                    Mono.error(BusinessException(AligoErrorCode.ALIGO_API_PARSE_ERROR))
                }
            }
            .onErrorMap { e ->
                when (e) {
                    is BusinessException -> e
                    else -> BusinessException(AligoErrorCode.ALIGO_API_SEND_ERROR)
                }
            }
    }
}