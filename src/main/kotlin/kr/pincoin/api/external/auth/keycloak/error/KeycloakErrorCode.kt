package kr.pincoin.api.external.auth.keycloak.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class KeycloakErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    // 인증 관련 오류
    INVALID_CREDENTIALS(
        HttpStatus.UNAUTHORIZED,
        "잘못된 아이디 또는 비밀번호입니다",
    ),
    INVALID_CLIENT(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 클라이언트 정보입니다",
    ),

    // 토큰 관련 오류
    INVALID_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "유효하지 않은 리프레시 토큰입니다",
    ),
    TOKEN_REFRESH_FAILED(
        HttpStatus.UNAUTHORIZED,
        "토큰 갱신에 실패했습니다",
    ),
    TOKEN_EXPIRED(
        HttpStatus.UNAUTHORIZED,
        "토큰이 만료되었습니다",
    ),
    TOKEN_REVOKED(
        HttpStatus.UNAUTHORIZED,
        "토큰이 무효화되었습니다",
    ),

    // 로그아웃 관련 오류
    LOGOUT_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "로그아웃 처리 중 오류가 발생했습니다",
    ),
    SESSION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "유효한 세션을 찾을 수 없습니다",
    ),

    // 관리자 토큰 관련 오류
    ADMIN_TOKEN_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 관리자 토큰 획득에 실패했습니다",
    ),
    ADMIN_TOKEN_EXPIRED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 관리자 토큰이 만료되었습니다",
    ),

    // 사용자 관리 관련 오류
    USER_CREATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 사용자 생성에 실패했습니다",
    ),
    USER_UPDATE_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 사용자 정보 수정에 실패했습니다",
    ),
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "Keycloak에서 사용자를 찾을 수 없습니다",
    ),
    USER_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "이미 존재하는 사용자입니다",
    ),

    // 네트워크 및 시스템 오류
    TIMEOUT(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 연결 타임아웃이 발생했습니다",
    ),
    CONNECTION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 연결에 실패했습니다",
    ),
    SYSTEM_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 서버 오류가 발생했습니다",
    ),
    CONFIGURATION_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 설정 오류가 발생했습니다",
    ),
    UNKNOWN(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Keycloak 알 수 없는 오류가 발생했습니다",
    ),

    // 권한 관련 오류
    INSUFFICIENT_PRIVILEGES(
        HttpStatus.FORBIDDEN,
        "Keycloak 작업에 필요한 권한이 부족합니다",
    ),
    REALM_ACCESS_DENIED(
        HttpStatus.FORBIDDEN,
        "Keycloak Realm에 대한 접근이 거부되었습니다",
    ),

    // 비밀번호 관련 오류
    PASSWORD_POLICY_VIOLATION(
        HttpStatus.BAD_REQUEST,
        "비밀번호가 정책을 만족하지 않습니다",
    ),
    PASSWORD_CHANGE_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "비밀번호 변경에 실패했습니다",
    ),

    // 이메일 관련 오류
    EMAIL_VERIFICATION_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "이메일 인증 처리에 실패했습니다",
    ),
    EMAIL_ALREADY_VERIFIED(
        HttpStatus.CONFLICT,
        "이미 인증된 이메일입니다",
    ),

    // Rate Limiting 관련 오류
    RATE_LIMIT_EXCEEDED(
        HttpStatus.TOO_MANY_REQUESTS,
        "요청 횟수 제한을 초과했습니다",
    ),
    BRUTE_FORCE_DETECTED(
        HttpStatus.TOO_MANY_REQUESTS,
        "비정상적인 로그인 시도가 감지되었습니다",
    ),
}