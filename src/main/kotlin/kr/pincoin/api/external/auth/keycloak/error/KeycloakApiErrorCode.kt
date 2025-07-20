package kr.pincoin.api.external.auth.keycloak.error

enum class KeycloakApiErrorCode(
    val status: Int,
    val code: String,
    val message: String
) {
    // HTTP 상태 코드 기반 에러
    BAD_REQUEST(400, "BAD_REQUEST", "잘못된 요청입니다"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    FORBIDDEN(403, "FORBIDDEN", "권한이 없습니다"),
    NOT_FOUND(404, "NOT_FOUND", "리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(405, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다"),
    CONFLICT(409, "CONFLICT", "리소스 충돌이 발생했습니다"),
    UNPROCESSABLE_ENTITY(422, "UNPROCESSABLE_ENTITY", "처리할 수 없는 요청입니다"),
    TOO_MANY_REQUESTS(429, "TOO_MANY_REQUESTS", "요청 한도를 초과했습니다"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
    BAD_GATEWAY(502, "BAD_GATEWAY", "게이트웨이 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(503, "SERVICE_UNAVAILABLE", "서비스를 사용할 수 없습니다"),
    GATEWAY_TIMEOUT(504, "GATEWAY_TIMEOUT", "게이트웨이 시간 초과"),

    // 네트워크 관련 에러
    TIMEOUT(408, "TIMEOUT", "요청 시간이 초과되었습니다"),
    CONNECTION_ERROR(0, "CONNECTION_ERROR", "네트워크 연결 오류가 발생했습니다"),
    DNS_ERROR(0, "DNS_ERROR", "DNS 조회 실패"),

    // 파싱 관련 에러
    JSON_PARSING_ERROR(0, "JSON_PARSING_ERROR", "JSON 파싱 오류가 발생했습니다"),
    RESPONSE_PARSING_ERROR(0, "RESPONSE_PARSING_ERROR", "응답 파싱 오류가 발생했습니다"),

    // Keycloak 특화 에러
    INVALID_GRANT(400, "INVALID_GRANT", "유효하지 않은 grant입니다"),
    INVALID_CLIENT(401, "INVALID_CLIENT", "유효하지 않은 클라이언트입니다"),
    INVALID_REQUEST(400, "INVALID_REQUEST", "유효하지 않은 요청입니다"),
    INVALID_SCOPE(400, "INVALID_SCOPE", "유효하지 않은 scope입니다"),
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    UNSUPPORTED_GRANT_TYPE(400, "UNSUPPORTED_GRANT_TYPE", "지원하지 않는 grant type입니다"),
    ACCESS_DENIED(403, "ACCESS_DENIED", "접근이 거부되었습니다"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS(409, "USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다"),
    USER_DISABLED(403, "USER_DISABLED", "비활성화된 사용자입니다"),
    ACCOUNT_TEMPORARILY_DISABLED(423, "ACCOUNT_TEMPORARILY_DISABLED", "계정이 일시적으로 비활성화되었습니다"),
    INVALID_USER_CREDENTIALS(401, "INVALID_USER_CREDENTIALS", "잘못된 사용자 자격 증명입니다"),
    PASSWORD_POLICY_NOT_MET(400, "PASSWORD_POLICY_NOT_MET", "비밀번호 정책을 만족하지 않습니다"),
    REALM_NOT_FOUND(404, "REALM_NOT_FOUND", "Realm을 찾을 수 없습니다"),
    CLIENT_NOT_FOUND(404, "CLIENT_NOT_FOUND", "클라이언트를 찾을 수 없습니다"),
    ROLE_NOT_FOUND(404, "ROLE_NOT_FOUND", "역할을 찾을 수 없습니다"),
    GROUP_NOT_FOUND(404, "GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다"),

    // 기타 에러
    UNKNOWN(500, "UNKNOWN", "알 수 없는 오류가 발생했습니다"),
    ADMIN_TOKEN_EXPIRED(401, "ADMIN_TOKEN_EXPIRED", "관리자 토큰이 만료되었습니다"),
    INSUFFICIENT_SCOPE(403, "INSUFFICIENT_SCOPE", "권한 범위가 부족합니다");

    companion object {
        /**
         * HTTP 상태 코드를 기반으로 적절한 에러 코드를 반환합니다.
         *
         * @param statusCode HTTP 상태 코드
         * @return 해당하는 KeycloakApiErrorCode
         */
        fun fromStatus(statusCode: Int): KeycloakApiErrorCode {
            return values().find { it.status == statusCode } ?: when (statusCode) {
                400 -> BAD_REQUEST
                401 -> UNAUTHORIZED
                403 -> FORBIDDEN
                404 -> NOT_FOUND
                405 -> METHOD_NOT_ALLOWED
                408 -> TIMEOUT
                409 -> CONFLICT
                422 -> UNPROCESSABLE_ENTITY
                423 -> ACCOUNT_TEMPORARILY_DISABLED
                429 -> TOO_MANY_REQUESTS
                500 -> INTERNAL_SERVER_ERROR
                502 -> BAD_GATEWAY
                503 -> SERVICE_UNAVAILABLE
                504 -> GATEWAY_TIMEOUT
                else -> UNKNOWN
            }
        }

        /**
         * Keycloak 에러 코드 문자열을 기반으로 적절한 에러 코드를 반환합니다.
         *
         * @param errorCode Keycloak에서 반환된 에러 코드 문자열
         * @return 해당하는 KeycloakApiErrorCode
         */
        fun fromKeycloakError(errorCode: String): KeycloakApiErrorCode {
            return when (errorCode.lowercase()) {
                "invalid_grant" -> INVALID_GRANT
                "invalid_client" -> INVALID_CLIENT
                "invalid_request" -> INVALID_REQUEST
                "invalid_scope" -> INVALID_SCOPE
                "invalid_token" -> INVALID_TOKEN
                "unsupported_grant_type" -> UNSUPPORTED_GRANT_TYPE
                "access_denied" -> ACCESS_DENIED
                "user_not_found" -> USER_NOT_FOUND
                "user_already_exists" -> USER_ALREADY_EXISTS
                "user_disabled" -> USER_DISABLED
                "account_temporarily_disabled" -> ACCOUNT_TEMPORARILY_DISABLED
                "invalid_user_credentials" -> INVALID_USER_CREDENTIALS
                "password_policy_not_met" -> PASSWORD_POLICY_NOT_MET
                "realm_not_found" -> REALM_NOT_FOUND
                "client_not_found" -> CLIENT_NOT_FOUND
                "role_not_found" -> ROLE_NOT_FOUND
                "group_not_found" -> GROUP_NOT_FOUND
                "admin_token_expired" -> ADMIN_TOKEN_EXPIRED
                "insufficient_scope" -> INSUFFICIENT_SCOPE
                else -> UNKNOWN
            }
        }

        /**
         * 예외 타입을 기반으로 적절한 에러 코드를 반환합니다.
         *
         * @param exception 발생한 예외
         * @return 해당하는 KeycloakApiErrorCode
         */
        fun fromException(exception: Exception): KeycloakApiErrorCode {
            return when (exception) {
                is java.net.SocketTimeoutException,
                is java.util.concurrent.TimeoutException -> TIMEOUT

                is java.net.ConnectException,
                is java.net.NoRouteToHostException -> CONNECTION_ERROR

                is java.net.UnknownHostException -> DNS_ERROR

                is com.fasterxml.jackson.core.JsonParseException,
                is com.fasterxml.jackson.databind.JsonMappingException -> JSON_PARSING_ERROR

                else -> UNKNOWN
            }
        }
    }
}