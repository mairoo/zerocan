package kr.pincoin.api.external.notification.telegram.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TelegramMessagePayload(
    @field:NotBlank(message = "메시지는 필수입니다")
    @field:Size(
        max = 4096,
        message = "메시지는 4096자를 초과할 수 없습니다"
    )
    val message: String,
)