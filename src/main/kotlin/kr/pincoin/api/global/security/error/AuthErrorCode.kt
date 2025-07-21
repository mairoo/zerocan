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
    EXPIRED_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "만료된 토큰입니다",
    ),
    INVALID_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 토큰입니다",
    ),
    INVALID_CREDENTIALS(
        HttpStatus.UNAUTHORIZED,
        "잘못된 아이디 또는 비밀번호입니다",
    ),
    INVALID_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 리프레시 토큰입니다",
    ),
    TOKEN_REFRESH_TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "리프레시 토큰 타임아웃이 발생했습니다",
    ),
    TOKEN_REFRESH_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "토큰 리프레시에 실패했습니다",
    ),

    KEYCLOAK_TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 타임아웃이 발생했습니다",
    ),
    KEYCLOAK_SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 오류가 발생했습니다",
    ),

    LOGIN_TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "로그인 타임아웃이 발생했습니다",
    ),
    AUTHENTICATION_FAILED(
        HttpStatus.UNAUTHORIZED,
        "인증에 실패했습니다",
    ),
    LOGIN_FAILED(
        HttpStatus.UNAUTHORIZED,
        "로그인에 실패했습니다",
    ),

    // API 키 인증 관련
    INVALID_API_KEY(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 API 키입니다",
    ),
    MISSING_API_KEY(
        HttpStatus.UNAUTHORIZED,
        "API 키가 없습니다",
    ),
    INVALID_SIGNATURE(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 서명입니다",
    ),
    MISSING_SIGNATURE(
        HttpStatus.UNAUTHORIZED,
        "서명이 없습니다",
    ),
    MISSING_TIMESTAMP(
        HttpStatus.UNAUTHORIZED,
        "타임스탬프가 없습니다",
    ),
    SIGNATURE_CREATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "서명 생성에 실패했습니다"
    ),
    EXPIRED_TIMESTAMP(
        HttpStatus.UNAUTHORIZED,
        "만료된 타임스탬프입니다",
    ),
    UNEXPECTED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "알 수 없는 인증 오류가 발생했습니다",
    ),
}