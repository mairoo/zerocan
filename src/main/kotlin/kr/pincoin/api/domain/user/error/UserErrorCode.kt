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
    INVALID_CREDENTIALS(
        HttpStatus.BAD_REQUEST,
        "이메일 또는 비밀번호 불일치"
    ),
    LOGIN_TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "로그인 타임아웃",
    ),
    LOGIN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "로그인 실패",
    ),
    SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "사용자 생성 중 시스템 오류가 발생했습니다",
    ),
    AUTHENTICATION_FAILED(
        HttpStatus.UNAUTHORIZED,
        "인증에 실패했습니다",
    ),
    AUTHENTICATION_PARSING_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "인증 응답 파싱 처리 중 오류가 발생했습니다",
    ),
    USER_CREATE_FAILED(
        HttpStatus.BAD_REQUEST,
        "Keycloak 사용자 생성에 실패했습니다",
    ),
    KEYCLOAK_PARSING_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 응답 파싱 처리 중 오류가 발생했습니다",
    ),
    ADMIN_TOKEN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 관리자 토큰 획득 실패했습니다",
    ),
}