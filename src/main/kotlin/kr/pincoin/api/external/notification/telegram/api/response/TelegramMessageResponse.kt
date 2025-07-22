package kr.pincoin.api.external.notification.telegram.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TelegramMessageResponse(
    @JsonProperty("message_id")
    val messageId: Long,

    @JsonProperty("sender_chat")
    val senderChat: TelegramChatResponse,

    @JsonProperty("chat")
    val chat: TelegramChatResponse,

    @JsonProperty("date")
    val date: Long,

    @JsonProperty("text")
    val text: String
)