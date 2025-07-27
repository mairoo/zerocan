package kr.pincoin.api.app.s3.admin.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HealthCheckResponse(
    @JsonProperty("status")
    val status: String,

    @JsonProperty("healthy")
    val healthy: Boolean,

    @JsonProperty("timestamp")
    val timestamp: String,

    @JsonProperty("service")
    val service: String,

    @JsonProperty("checks")
    val checks: List<String>? = null,
) {
    companion object {
        fun of(
            checks: List<String> = emptyList(),
        ) = HealthCheckResponse(
            status = "UP",
            healthy = true,
            timestamp = LocalDateTime.now().toString(),
            service = "S3",
            checks = checks,
        )
    }
}