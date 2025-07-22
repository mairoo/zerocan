package kr.pincoin.api.external.notification.slack.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 슬랙 메시지 상세 정보
 * - type: 메시지 타입
 * - text: 메시지 내용
 * - user: 사용자 ID
 * - team: 팀 ID
 * - botId: 봇 ID
 * - ts: 메시지 타임스탬프
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SlackMessageDetail(
    @JsonProperty("type")
    val type: String = "message",

    @JsonProperty("text")
    val text: String,

    @JsonProperty("user")
    val user: String? = null,

    @JsonProperty("team")
    val team: String? = null,

    @JsonProperty("bot_id")
    val botId: String? = null,

    @JsonProperty("ts")
    val ts: String
)