package kr.pincoin.api.external.notification.telegram.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TelegramChatResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("type")
    val type: String
)