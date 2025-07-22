package kr.pincoin.api.external.notification.telegram.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.external.notification.telegram.api.request.TelegramMessagePayload
import kr.pincoin.api.external.notification.telegram.api.request.TelegramMessageRequest
import kr.pincoin.api.external.notification.telegram.error.TelegramErrorCode
import kr.pincoin.api.external.notification.telegram.properties.TelegramProperties
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class TelegramMessageApiClient(
    private val telegramWebClient: WebClient,
    private val telegramProperties: TelegramProperties,
) {
    private val log = KotlinLogging.logger {}

    fun sendMessage(messageText: TelegramMessagePayload): Mono<Boolean> {
        val request = TelegramMessageRequest.of(telegramProperties.chatId, messageText)

        return telegramWebClient
            .post()
            .uri("/bot${telegramProperties.botToken}/sendMessage")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            // 응답 코드가 2xx면 성공으로 간주
            .onStatus(
                { status -> status.is4xxClientError || status.is5xxServerError },
                { response ->
                    response.bodyToMono(String::class.java)
                        .flatMap { errorBody ->
                            log.warn { "텔레그램 API 오류 응답: $errorBody" }
                            Mono.error(
                                BusinessException(
                                    TelegramErrorCode.TELEGRAM_API_SEND_ERROR,
                                    "상세 오류: $errorBody"
                                )
                            )
                        }
                }
            )
            // 응답 본문은 무시하고 성공 여부만 반환
            .bodyToMono(String::class.java)
            .doOnNext { log.debug { "${"텔레그램 응답: {}"} $it" } }
            .map { true } // 응답 내용 무시하고 성공 여부만 반환
            .onErrorResume { e ->
                log.error(e) { "텔레그램 메시지 전송 중 오류 발생" }
                Mono.just(false) // 오류 발생 시 false 반환
            }
    }
}