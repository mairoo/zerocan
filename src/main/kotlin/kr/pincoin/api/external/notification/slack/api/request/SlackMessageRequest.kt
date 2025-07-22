package kr.pincoin.api.external.notification.slack.api.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 슬랙 메시지 요청
 * - channel: 메시지를 전송할 채널 ID 또는 이름
 * - text: 전송할 메시지 내용
 * - mrkdwn: 마크다운 파싱 여부 (기본값: true)
 */
data class SlackMessageRequest(
    @field:JsonProperty("channel")
    val channel: String,

    @field:JsonProperty("text")
    val text: String,

    @field:JsonProperty("mrkdwn")
    val mrkdwn: Boolean = true
) {
    companion object {
        fun of(channel: String, messageText: SlackMessagePayload): SlackMessageRequest {
            return SlackMessageRequest(
                channel = channel,
                text = messageText.message
            )
        }
    }
}