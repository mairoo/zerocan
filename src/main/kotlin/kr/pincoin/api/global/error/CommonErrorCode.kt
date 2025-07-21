package kr.pincoin.api.global.error

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    INVALID_INPUT_VALUE(
        HttpStatus.BAD_REQUEST,
        "잘못된 입력값입니다.",
    ),
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST,
        "잘못된 요청입니다.",
    ),
    METHOD_NOT_ALLOWED(
        HttpStatus.METHOD_NOT_ALLOWED,
        "지원하지 않는 HTTP 메서드입니다",
    ),
    REQUEST_BODY_MISSING(
        HttpStatus.BAD_REQUEST,
        "요청 본문이 없거나 형식이 잘못되었습니다",
    ),
    REQUEST_COOKIE_MISSING(
        HttpStatus.BAD_REQUEST,
        "요청 쿠키가 없습니다.",
    ),
    UNSUPPORTED_MEDIA_TYPE(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "지원하지 않는 미디어 타입입니다",
    ),
    FILE_SIZE_EXCEEDED(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "업로드 파일 크기가 제한을 초과했습니다",
    ),
    NULL_POINTER_EXCEPTION(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "객체 참조 오류가 발생했습니다",
    ),
    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "서버 오류가 발생했습니다.",
    ),
    DATA_INTEGRITY_VIOLATION(
        HttpStatus.BAD_REQUEST,
        "데이터 제약조건을 위반했습니다",
    ),
    DUPLICATE_KEY(
        HttpStatus.CONFLICT,
        "이미 존재하는 데이터입니다",
    ),
    FOREIGN_KEY_VIOLATION(
        HttpStatus.BAD_REQUEST,
        "참조하는 데이터가 존재하지 않습니다",
    ),
    RESOURCE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "요청한 리소스를 찾을 수 없습니다",
    ),
}