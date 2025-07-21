package kr.pincoin.api.global.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.pincoin.api.global.response.error.ErrorResponse
import kr.pincoin.api.global.security.error.AuthErrorCode
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class ApiAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.apply {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = "${MediaType.APPLICATION_JSON_VALUE};charset=UTF-8"
            characterEncoding = "UTF-8"

            val errorResponse = ErrorResponse.of(
                request = request,
                status = AuthErrorCode.UNAUTHORIZED.status,
                message = AuthErrorCode.UNAUTHORIZED.message
            )

            objectMapper.writeValue(writer, errorResponse)
        }
    }
}