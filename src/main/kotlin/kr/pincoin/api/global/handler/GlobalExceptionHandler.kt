package kr.pincoin.api.global.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.global.response.error.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = KotlinLogging.logger {}

    // handleAuthenticationException 메소드 - 재정의 안 함 401: JwtAuthenticationFilter 필터에서 직접 예외 처리
    // handleAccessDeniedException 메소드 - 재정의 안 함 403: 스프링 시큐리티 커스텀 예외 처리

    /**
     * BusinessException 예외 핸들러 메서드
     *
     * @param e 발생한 BusinessException
     * @param request HTTP 요청 정보
     * @return 에러 응답 엔티티
     */
    @ExceptionHandler(BusinessException::class)
    private fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(
                ErrorResponse.Companion.of(
                    request = request,
                    status = e.errorCode.status,
                    message = e.message ?: e.errorCode.message
                )
            )
            .also { log.error { "${e.message} ${e.errorCode.message}" } }
}