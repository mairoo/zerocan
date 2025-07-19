package kr.pincoin.api.global.response.cursor

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CursorResponse<T>(
    val content: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            nextCursor: String?,
            hasNext: Boolean,
        ) = CursorResponse(
            content = content,
            nextCursor = nextCursor,
            hasNext = hasNext
        )
    }
}