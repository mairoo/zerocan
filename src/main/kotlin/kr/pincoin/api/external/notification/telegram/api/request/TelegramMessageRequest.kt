package kr.pincoin.api.external.notification.telegram.api.request

import com.fasterxml.jackson.annotation.JsonProperty

data class TelegramMessageRequest(
    @field:JsonProperty("chat_id")
    val chatId: String,

    @field:JsonProperty("text")
    val text: String,

    @field:JsonProperty("parse_mode")
    val parseMode: String = "HTML"
) {
    companion object {
        fun of(chatId: String, messageText: TelegramMessagePayload): TelegramMessageRequest {
            return TelegramMessageRequest(
                chatId = chatId,
                text = messageText.message
            )
        }
    }
}