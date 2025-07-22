package kr.pincoin.api.external.notification.slack.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 슬랙 메시지 응답
 * - ts: 메시지 타임스탬프 (ID로 사용)
 * - channel: 채널 정보
 * - message: 메시지 정보
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SlackMessageResponse(
    @JsonProperty("ts")
    val ts: String,

    @JsonProperty("channel")
    val channel: String,

    @JsonProperty("message")
    val message: SlackMessageDetail? = null
)