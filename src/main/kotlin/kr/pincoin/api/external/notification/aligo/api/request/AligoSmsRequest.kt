package kr.pincoin.api.external.notification.aligo.api.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AligoSmsRequest(
    @field:Pattern(
        regexp = "^01[016789]\\d{7,8}$",
        message = "올바른 휴대전화 번호 형식이 아닙니다 (하이픈 제외 10-11자리)"
    )
    val receiver: String,

    @field:Size(
        max = 1000,
        message = "메시지는 1000자를 초과할 수 없습니다"
    )
    val message: String
)