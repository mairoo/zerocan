package kr.pincoin.api.external.notification.aligo.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AligoSmsResponse(
    @JsonProperty("result_code")
    val resultCode: String,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("msg_id")
    val msgId: String,

    @JsonProperty("success_cnt")
    val successCount: String,

    @JsonProperty("error_cnt")
    val errorCount: String,

    @JsonProperty("msg_type")
    val msgType: String
)