package kr.pincoin.api.external.auth.recaptcha.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class RecaptchaErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    VERIFICATION_FAILED(
        HttpStatus.BAD_REQUEST,
        "reCAPTCHA 검증에 실패했습니다",
    ),
    INVALID_TOKEN(
        HttpStatus.BAD_REQUEST,
        "유효하지 않은 reCAPTCHA 토큰입니다",
    ),
    TIMEOUT_OR_DUPLICATE(
        HttpStatus.BAD_REQUEST,
        "reCAPTCHA 토큰이 만료되었거나 이미 사용되었습니다",
    ),
    HOSTNAME_MISMATCH(
        HttpStatus.BAD_REQUEST,
        "reCAPTCHA 호스트명이 일치하지 않습니다",
    ),
    LOW_SCORE(
        HttpStatus.BAD_REQUEST,
        "reCAPTCHA 점수가 기준점 미달입니다",
    ),
    NETWORK_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "reCAPTCHA 서버 연결에 실패했습니다",
    ),
    SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "reCAPTCHA 시스템 오류가 발생했습니다",
    ),
    UNKNOWN(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "reCAPTCHA 알 수 없는 오류가 발생했습니다",
    ),
}