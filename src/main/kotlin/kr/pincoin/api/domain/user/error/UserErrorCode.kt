package kr.pincoin.api.domain.user.error

import kr.pincoin.api.global.security.exception.error.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "유효한 사용자가 없습니다",
    ),
    INACTIVE(
        HttpStatus.CONFLICT,
        "사용자가 비활성화 상태입니다",
    ),
    ALREADY_EXISTS(
        HttpStatus.BAD_REQUEST,
        "아이디 또는 이메일이 이미 존재합니다",
    ),
    PASSWORD_MISMATCH(
        HttpStatus.BAD_REQUEST,
        "비밀번호 불일치"
    ),
}