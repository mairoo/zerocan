package kr.pincoin.api.global.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import kr.pincoin.api.global.error.CommonErrorCode
import kr.pincoin.api.global.exception.BusinessException
import kr.pincoin.api.global.response.error.ErrorResponse
import kr.pincoin.api.global.security.error.AuthErrorCode
import org.apache.coyote.BadRequestException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.sql.SQLIntegrityConstraintViolationException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = KotlinLogging.logger {}

    // handleAuthenticationException 메소드 - 재정의 안 함 401: JwtAuthenticationFilter 필터에서 직접 예외 처리
    // handleAccessDeniedException 메소드 - 재정의 안 함 403: 스프링 시큐리티 커스텀 예외 처리

    // ========================================
    // 1. 비즈니스 예외 처리
    // ========================================

    /**
     * BusinessException 예외 핸들러 메서드
     */
    @ExceptionHandler(BusinessException::class)
    private fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(
                ErrorResponse.of(
                    request = request,
                    status = e.errorCode.status,
                    message = e.message ?: e.errorCode.message
                )
            )
            .also { log.error { "${e.message} ${e.errorCode.message}" } }

    // ========================================
    // 2. 인증/인가 관련 예외 처리
    // ========================================

    /**
     * AuthorizationDeniedException 예외 핸들러 메서드
     *
     * @param e 발생한 AuthorizationDeniedException
     * @param request HTTP 요청 정보
     * @return 권한 없음 에러 응답 엔티티
     */
    @ExceptionHandler(AuthorizationDeniedException::class)
    private fun handleAuthorizationDeniedException(
        e: AuthorizationDeniedException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(AuthErrorCode.FORBIDDEN.status)
        .body(
            ErrorResponse.of(
                request,
                AuthErrorCode.FORBIDDEN.status,
                AuthErrorCode.FORBIDDEN.message
            )
        )
        .also { log.error { "[Authorization Denied] ${e.message}" } }

    /**
     * NullPointerException 예외 핸들러 메서드
     *
     * @param e 발생한 NullPointerException
     * @param request HTTP 요청 정보
     * @return 에러 응답 엔티티
     *         - User.getId()가 null인 경우: 인증 필요 에러 응답
     *         - 그 외의 경우: 내부 서버 오류 응답
     */
    @ExceptionHandler(NullPointerException::class)
    private fun handleNullPointerException(
        e: NullPointerException,
        request: HttpServletRequest,
    ) = when {
        e.message?.contains("User.getId()") == true -> {
            log.error { "[Authentication Required] ${e.message}" }
            ResponseEntity
                .status(AuthErrorCode.UNAUTHORIZED.status)
                .body(
                    ErrorResponse.of(
                        request,
                        AuthErrorCode.UNAUTHORIZED.status,
                        AuthErrorCode.UNAUTHORIZED.message
                    )
                )
        }

        else -> {
            log.error(e) { "[NullPointerException] ${e.message}, Stack trace: ${e.stackTraceToString()}" }
            ResponseEntity
                .status(CommonErrorCode.NULL_POINTER_EXCEPTION.status)
                .body(
                    ErrorResponse.of(
                        request,
                        CommonErrorCode.NULL_POINTER_EXCEPTION.status,
                        CommonErrorCode.NULL_POINTER_EXCEPTION.message
                    )
                )
        }
    }

    // ========================================
    // 3. 요청 검증 관련 예외 처리
    // ========================================

    /**
     * MethodArgumentNotValidException 예외 핸들러 메서드
     *
     * @param e 발생한 MethodArgumentNotValidException
     * @param request HTTP 요청 정보
     * @return 유효하지 않은 입력값 에러 응답 엔티티 (검증 오류 정보 포함)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    private fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error { "[Validation Exception] ${e.message}" }

        val validationErrors = e.bindingResult.fieldErrors.map { error ->
            ErrorResponse.ValidationError(
                field = error.field,
                message = error.defaultMessage.orEmpty()
            )
        }

        return ResponseEntity
            .status(CommonErrorCode.INVALID_INPUT_VALUE.status)
            .body(
                ErrorResponse.of(
                    request,
                    CommonErrorCode.INVALID_INPUT_VALUE.status,
                    CommonErrorCode.INVALID_INPUT_VALUE.message,
                    validationErrors
                )
            )
    }

    /**
     * IllegalArgumentException 예외 핸들러 메서드
     */
    @ExceptionHandler(IllegalArgumentException::class)
    private fun handleIllegalArgumentException(
        e: IllegalArgumentException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.INVALID_INPUT_VALUE.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.INVALID_INPUT_VALUE.status,
                e.message ?: CommonErrorCode.INVALID_INPUT_VALUE.message
            )
        )
        .also { log.error { "[Illegal Argument] ${e.message}" } }

    /**
     * MethodArgumentTypeMismatchException 예외 핸들러 메서드
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    private fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.INVALID_INPUT_VALUE.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.INVALID_INPUT_VALUE.status,
                "잘못된 파라미터 타입입니다: ${e.name}"
            )
        )
        .also { log.error { "[Type Mismatch] ${e.message}" } }

    // ========================================
    // 4. HTTP 요청 관련 예외 처리
    // ========================================

    /**
     * BadRequestException 예외 핸들러 메서드
     *
     * @param e 발생한 BadRequestException
     * @param request HTTP 요청 정보
     * @return 유효하지 않은 요청 에러 응답 엔티티
     */
    @ExceptionHandler(BadRequestException::class)
    private fun handleBadRequestException(
        e: BadRequestException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> = ResponseEntity
        .status(CommonErrorCode.INVALID_REQUEST.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.INVALID_REQUEST.status,
                message = e.message ?: CommonErrorCode.INVALID_REQUEST.message
            )
        )
        .also { log.error { "[Bad Request Exception] ${e.message}" } }

    /**
     * HttpMessageNotReadableException 예외 핸들러 메서드
     *
     * @param e 발생한 HttpMessageNotReadableException
     * @param request HTTP 요청 정보
     * @return 요청 본문 누락 에러 응답 엔티티
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    private fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.REQUEST_BODY_MISSING.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.REQUEST_BODY_MISSING.status,
                CommonErrorCode.REQUEST_BODY_MISSING.message
            )
        )
        .also { log.error { "[Request Body Missing] ${e.message}" } }

    /**
     * MissingRequestCookieException 예외 핸들러 메서드
     *
     * @param e 발생한 MissingRequestCookieException
     * @param request HTTP 요청 정보
     * @return 요청 쿠키 누락 에러 응답 엔티티
     */
    @ExceptionHandler(MissingRequestCookieException::class)
    private fun handleMissingRequestCookieException(
        e: MissingRequestCookieException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.REQUEST_COOKIE_MISSING.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.REQUEST_COOKIE_MISSING.status,
                message = CommonErrorCode.REQUEST_COOKIE_MISSING.message
            )
        )
        .also { log.error { "[Missing Cookie] ${e.message}" } }

    /**
     * MissingServletRequestParameterException 예외 핸들러 메서드
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    private fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.INVALID_INPUT_VALUE.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.INVALID_INPUT_VALUE.status,
                "필수 파라미터가 누락되었습니다: ${e.parameterName}"
            )
        )
        .also { log.error { "[Missing Parameter] ${e.message}" } }

    /**
     * HttpRequestMethodNotSupportedException 예외 핸들러 메서드
     *
     * @param e 발생한 HttpRequestMethodNotSupportedException
     * @param request HTTP 요청 정보
     * @return 허용되지 않은 HTTP 메서드 에러 응답 엔티티
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    private fun handleHttpRequestMethodNotSupportedException(
        e: HttpRequestMethodNotSupportedException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.METHOD_NOT_ALLOWED.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.METHOD_NOT_ALLOWED.status,
                message = CommonErrorCode.METHOD_NOT_ALLOWED.message
            )
        )
        .also { log.error { "[Method Not Allowed] ${e.message}" } }

    /**
     * HttpMediaTypeNotSupportedException 예외 핸들러 메서드
     *
     * @param e 발생한 HttpMediaTypeNotSupportedException
     * @param request HTTP 요청 정보
     * @return 지원되지 않는 미디어 타입 에러 응답 엔티티
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    private fun handleHttpMediaTypeNotSupportedException(
        e: HttpMediaTypeNotSupportedException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.status,
                message = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.message
            )
        )
        .also { log.error { "[Unsupported Media Type] ${e.message}" } }

    /**
     * MaxUploadSizeExceededException 예외 핸들러 메서드
     *
     * @param e 발생한 MaxUploadSizeExceededException
     * @param request HTTP 요청 정보
     * @return 파일 크기 초과 에러 응답 엔티티
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    private fun handleMaxUploadSizeExceededException(
        e: MaxUploadSizeExceededException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.FILE_SIZE_EXCEEDED.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.FILE_SIZE_EXCEEDED.status,
                message = CommonErrorCode.FILE_SIZE_EXCEEDED.message
            )
        )
        .also { log.error { "[File Size Exceeded] ${e.message}" } }

    // ========================================
    // 5. 리소스 관련 예외 처리
    // ========================================

    /**
     * NoResourceFoundException 예외 핸들러 메서드
     *
     * @param e 발생한 NoResourceFoundException
     * @param request HTTP 요청 정보
     * @return 리소스 찾을 수 없음 에러 응답 엔티티
     */
    @ExceptionHandler(NoResourceFoundException::class)
    private fun handleNoResourceFoundException(
        e: NoResourceFoundException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.RESOURCE_NOT_FOUND.status)
        .body(
            ErrorResponse.of(
                request = request,
                status = CommonErrorCode.RESOURCE_NOT_FOUND.status,
                message = CommonErrorCode.RESOURCE_NOT_FOUND.message
            )
        )
        .also { log.error { "[Page Not Found] ${e.message}" } }

    /**
     * EntityNotFoundException 예외 핸들러 메서드
     *
     * @param e 발생한 EntityNotFoundException
     * @param request HTTP 요청 정보
     * @return 엔티티를 찾을 수 없음 에러 응답 엔티티
     */
    @ExceptionHandler(EntityNotFoundException::class)
    private fun handleEntityNotFoundException(
        e: EntityNotFoundException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.RESOURCE_NOT_FOUND.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.RESOURCE_NOT_FOUND.status,
                CommonErrorCode.RESOURCE_NOT_FOUND.message
            )
        )
        .also { log.error { "[Resource Not Found] ${e.message}" } }

    // ========================================
    // 6. 데이터베이스 관련 예외 처리
    // ========================================

    /**
     * DataIntegrityViolationException 예외 핸들러 메서드
     *
     * @param e 발생한 DataIntegrityViolationException
     * @param request HTTP 요청 정보
     * @return 데이터 무결성 위반 에러 응답 엔티티
     *         - 중복 키 오류인 경우: 중복 키 에러 응답
     *         - 외래 키 제약 조건 위반인 경우: 외래 키 제약 조건 위반 에러 응답
     *         - 그 외의 경우: 데이터 무결성 위반 에러 응답
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    private fun handleDataIntegrityViolationException(
        e: DataIntegrityViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error { "[Data Integrity Violation] Message: ${e.message}, Cause: ${e.mostSpecificCause.message}" }

        return when (val cause = e.cause) {
            is SQLIntegrityConstraintViolationException -> when (cause.errorCode) {
                1062 -> CommonErrorCode.DUPLICATE_KEY
                1452 -> CommonErrorCode.FOREIGN_KEY_VIOLATION
                else -> CommonErrorCode.DATA_INTEGRITY_VIOLATION
            }

            else -> CommonErrorCode.DATA_INTEGRITY_VIOLATION
        }.let { errorCode ->
            ResponseEntity
                .status(errorCode.status)
                .body(ErrorResponse.of(request, errorCode.status, errorCode.message))
        }
    }

    // ========================================
    // 7. 시스템 관련 예외 처리
    // ========================================

    /**
     * IllegalStateException 예외 핸들러 메서드
     *
     * @param e 발생한 IllegalStateException
     * @param request HTTP 요청 정보
     * @return 서버 상태 오류 응답 엔티티
     */
    @ExceptionHandler(IllegalStateException::class)
    private fun handleIllegalStateException(
        e: IllegalStateException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.INTERNAL_SERVER_ERROR.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.INTERNAL_SERVER_ERROR.status,
                "서버 상태 오류가 발생했습니다"
            )
        )
        .also { log.error { "[Illegal State] ${e.message}" } }

    /**
     * AsyncRequestTimeoutException 예외 핸들러 메서드
     */
    @ExceptionHandler(AsyncRequestTimeoutException::class)
    private fun handleAsyncRequestTimeoutException(
        e: AsyncRequestTimeoutException,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ErrorResponse.of(
                request,
                HttpStatus.SERVICE_UNAVAILABLE,
                "요청 처리 시간이 초과되었습니다"
            )
        )
        .also { log.error { "[Async Timeout] ${e.message}" } }

    /**
     * 예기치 못한 내부 서버 오류를 처리하는 메서드
     *
     * @param e 발생한 Exception
     * @param request HTTP 요청 정보
     * @return 내부 서버 오류 응답 엔티티
     */
    @ExceptionHandler(Exception::class)
    private fun handleException(
        e: Exception,
        request: HttpServletRequest,
    ) = handleInternalServerError(e, request)

    private fun handleInternalServerError(
        e: Exception,
        request: HttpServletRequest,
    ) = ResponseEntity
        .status(CommonErrorCode.INTERNAL_SERVER_ERROR.status)
        .body(
            ErrorResponse.of(
                request,
                CommonErrorCode.INTERNAL_SERVER_ERROR.status,
                CommonErrorCode.INTERNAL_SERVER_ERROR.message
            )
        )
        .also { log.error(e) { "[Internal Server Error] ${e.message}, Stack trace: ${e.stackTraceToString()}" } }
}