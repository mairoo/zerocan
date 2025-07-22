package kr.pincoin.api.external.notification.aligo.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AligoErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    ALIGO_API_PARSE_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "알리고 API 응답 파싱 실패"
    ),
    ALIGO_API_SEND_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "알리고 SMS 발송 실패"
    ),
}