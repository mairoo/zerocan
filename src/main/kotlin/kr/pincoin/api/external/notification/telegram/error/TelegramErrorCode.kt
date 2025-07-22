package kr.pincoin.api.external.notification.telegram.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class TelegramErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    TELEGRAM_API_SEND_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "텔레그램 메시지 발송 실패"
    ),
}