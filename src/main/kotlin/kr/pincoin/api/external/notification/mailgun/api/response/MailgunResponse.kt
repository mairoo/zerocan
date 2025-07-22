package kr.pincoin.api.external.notification.mailgun.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MailgunResponse(
    @JsonProperty("id")
    val id: String? = null,

    @JsonProperty("message")
    val message: String? = null,

    @JsonProperty("status")
    val status: Int? = null
)