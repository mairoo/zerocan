package kr.pincoin.api.external.notification.mailgun.api.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MailgunRequest(
    @field:NotBlank(message = "수신자 이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val to: String,

    @field:NotBlank(message = "제목은 필수입니다")
    @field:Size(
        max = 255,
        message = "제목은 255자를 초과할 수 없습니다"
    )
    val subject: String,

    @field:NotBlank(message = "본문은 필수입니다")
    @field:Size(
        max = 10000,
        message = "본문은 10000자를 초과할 수 없습니다"
    )
    val text: String,

    @field:Size(
        max = 50000,
        message = "HTML 본문은 50000자를 초과할 수 없습니다"
    )
    val html: String? = null
)