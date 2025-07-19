package kr.pincoin.api.global.response.error

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val timestamp: Long = System.currentTimeMillis(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val errors: List<ValidationError>? = null
) {
    data class ValidationError(
        val field: String,
        val message: String
    )

    companion object {
        fun of(
            request: HttpServletRequest,
            status: HttpStatus,
            message: String,
            errors: List<ValidationError>? = null
        ) = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI,
            errors = errors
        )
    }
}
