package kr.pincoin.api.global.security.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
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
    INVALID_CREDENTIALS(
        HttpStatus.UNAUTHORIZED,
        "잘못된 아이디 또는 비밀번호입니다",
    ),
}