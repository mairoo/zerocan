package kr.pincoin.api.external.notification.telegram.service

import kr.pincoin.api.external.notification.telegram.api.request.TelegramMessagePayload
import kr.pincoin.api.external.notification.telegram.event.TelegramErrorNotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class TelegramNotificationListener(
    private val telegramMessageApiClient: TelegramMessageApiClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Async
    @EventListener
    fun handleErrorNotification(event: TelegramErrorNotificationEvent) {
        try {
            val messageBuilder = StringBuilder()
            messageBuilder.append("<b>⚠️ 오류 알림</b>\n\n")
            messageBuilder.append("<b>서비스:</b> ${event.serviceName}\n")
            messageBuilder.append("<b>발생 시간:</b> ${event.occurred.format(dateTimeFormatter)}\n")
            messageBuilder.append("<b>오류 소스:</b> ${event.errorSource}\n")

            event.errorCode?.let {
                messageBuilder.append("<b>오류 코드:</b> $it\n")
            }

            messageBuilder.append("<b>오류 메시지:</b> ${event.errorMessage}\n")

            event.transactionId?.let {
                messageBuilder.append("<b>트랜잭션 ID:</b> $it\n")
            }

            // 스택트레이스는 길이 제한하여 추가
            event.errorDetails?.let { details ->
                val truncatedDetails = if (details.length > 1000) {
                    details.take(1000) + "\n... (생략됨)"
                } else {
                    details
                }
                messageBuilder.append("\n<b>상세 정보:</b>\n$truncatedDetails")
            }

            // 최종 메시지 길이 체크
            val finalMessage = messageBuilder.toString()
            val payload = if (finalMessage.length > 4000) {
                // 4000자 초과시 스택트레이스 제거
                val messageWithoutDetails = messageBuilder.toString()
                    .substringBefore("\n<b>상세 정보:</b>")
                TelegramMessagePayload("$messageWithoutDetails\n\n<i>* 상세 정보는 로그를 확인해주세요.</i>")
            } else {
                TelegramMessagePayload(finalMessage)
            }

            telegramMessageApiClient.sendMessage(payload)
                .subscribe(
                    { success ->
                        if (success) {
                            logger.info("텔레그램 알림 전송 성공")
                        } else {
                            logger.warn("텔레그램 알림 전송 실패")
                        }
                    },
                    { error -> logger.error("텔레그램 알림 전송 중 예외 발생", error) }
                )
        } catch (e: Exception) {
            logger.error("텔레그램 알림 처리 중 오류 발생", e)
        }
    }
}