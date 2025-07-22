package kr.pincoin.api.external.auth.keycloak.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class KeycloakErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    INVALID_CREDENTIALS(
        HttpStatus.UNAUTHORIZED,
        "잘못된 아이디 또는 비밀번호입니다",
    ),
    INVALID_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 리프레시 토큰입니다",
    ),
    ADMIN_TOKEN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 관리자 토큰 획득에 실패했습니다",
    ),
    TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 연결 타임아웃이 발생했습니다",
    ),
    SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 오류가 발생했습니다",
    ),
    UNKNOWN(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 알 수 없는 오류가 발생했습니다",
    ),
}