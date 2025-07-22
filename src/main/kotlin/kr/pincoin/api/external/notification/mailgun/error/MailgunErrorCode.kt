package kr.pincoin.api.external.notification.mailgun.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class MailgunErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    MAILGUN_API_PARSE_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "mailgun API 응답 파싱 실패"
    ),
    MAILGUN_API_SEND_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "mailgun 이메일 발송 실패"
    ),
}