package kr.pincoin.api.external.notification.slack.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class SlackErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    SLACK_API_PARSE_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "슬랙 API 응답 파싱 실패"
    ),
    SLACK_API_SEND_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "슬랙 메시지 발송 실패"
    ),
}