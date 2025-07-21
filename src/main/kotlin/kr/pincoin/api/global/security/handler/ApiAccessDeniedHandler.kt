package kr.pincoin.api.global.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.pincoin.api.global.response.error.ErrorResponse
import kr.pincoin.api.global.security.error.AuthErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class ApiAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.apply {
            status = HttpStatus.FORBIDDEN.value()
            contentType = "${MediaType.APPLICATION_JSON_VALUE};charset=UTF-8"
            characterEncoding = "UTF-8"

            val errorResponse = ErrorResponse.of(
                request = request,
                status = AuthErrorCode.FORBIDDEN.status,
                message = AuthErrorCode.FORBIDDEN.message,
            )

            objectMapper.writeValue(writer, errorResponse)
        }
    }
}