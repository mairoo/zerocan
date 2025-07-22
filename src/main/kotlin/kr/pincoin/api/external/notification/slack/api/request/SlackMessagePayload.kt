package kr.pincoin.api.external.notification.slack.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 슬랙 메시지 페이로드
 * - message: 전송할 메시지 내용 (최대 40,000자)
 */
data class SlackMessagePayload(
    @field:NotBlank(message = "메시지는 필수입니다")
    @field:Size(
        max = 40000,
        message = "메시지는 40,000자를 초과할 수 없습니다"
    )
    val message: String,
)