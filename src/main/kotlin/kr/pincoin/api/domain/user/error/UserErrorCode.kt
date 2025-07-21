package kr.pincoin.api.domain.user.error

import kr.pincoin.api.global.error.ErrorCode
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
        HttpStatus.CONFLICT,
        "아이디 또는 이메일이 이미 존재합니다",
    ),
    SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "사용자 처리 중 시스템 오류가 발생했습니다",
    ),
    USER_CREATE_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "사용자 생성에 실패했습니다",
    ),
    ADMIN_TOKEN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "관리자 토큰 획득에 실패했습니다",
    ),
    LOGOUT_ALL_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "전체 로그아웃에 실패했습니다",
    ),
}