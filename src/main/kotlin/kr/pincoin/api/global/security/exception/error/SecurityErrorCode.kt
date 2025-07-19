package kr.pincoin.api.global.security.exception.error

import org.springframework.http.HttpStatus

enum class SecurityErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "해당 리소스에 대한 권한이 없습니다",
    ),
    UNAUTHORIZED(
        HttpStatus.UNAUTHORIZED,
        "로그인이 필요한 서비스입니다",
    ),
}