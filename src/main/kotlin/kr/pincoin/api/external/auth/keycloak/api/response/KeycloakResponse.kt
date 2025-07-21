package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * Keycloak API 응답의 공통 결과 타입
 * WizzpayResponse 패턴을 따라 구현
 */
sealed class KeycloakResponse<out T> {
    /**
     * 성공 응답
     * @param data 성공 시 반환할 데이터
     */
    data class Success<T>(val data: T) : KeycloakResponse<T>()

    /**
     * 실패 응답
     * @param errorCode 에러 코드
     * @param errorMessage 에러 메시지
     */
    data class Error<T>(
        val errorCode: String,
        val errorMessage: String
    ) : KeycloakResponse<T>()
}