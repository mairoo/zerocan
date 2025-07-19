package kr.pincoin.api.global.response.success

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val timestamp: Long = System.currentTimeMillis(),
    val status: Int,
    val message: String,
    val data: T
) {
    companion object {
        fun <T> of(
            data: T,
            status: HttpStatus = HttpStatus.OK,
            message: String = status.reasonPhrase
        ) = ApiResponse(
            status = status.value(),
            message = message,
            data = data
        )
    }
}