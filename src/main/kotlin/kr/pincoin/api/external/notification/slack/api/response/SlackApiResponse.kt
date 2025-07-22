package kr.pincoin.api.external.notification.slack.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 슬랙 API 공통 응답
 * - ok: API 호출 성공 여부
 * - error: 에러 메시지 (실패 시)
 * - warning: 경고 메시지
 * - result: 응답 결과
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SlackApiResponse<T>(
    @JsonProperty("ok")
    val ok: Boolean,

    @JsonProperty("error")
    val error: String? = null,

    @JsonProperty("warning")
    val warning: String? = null,

    @JsonProperty("result")
    val result: T? = null
)