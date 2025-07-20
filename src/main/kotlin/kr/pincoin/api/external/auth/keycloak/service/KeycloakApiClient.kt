package kr.pincoin.api.external.auth.keycloak.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.auth.keycloak.api.request.*
import kr.pincoin.api.external.auth.keycloak.api.response.*
import kr.pincoin.api.external.auth.keycloak.error.KeycloakApiErrorCode
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody

@Component
class KeycloakApiClient(
    private val keycloakWebClient: WebClient,
    private val keycloakProperties: KeycloakProperties,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Admin API - 사용자 생성
     * JSON 기반 POST 요청, Bearer 토큰 인증이 필요하므로 executeAdminApiCall 사용
     */
    suspend fun createUser(
        adminToken: String,
        request: KeycloakCreateUserRequest,
    ): KeycloakResponse = executeAdminApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users",
        method = "POST",
        adminToken = adminToken,
        request = request,
        responseType = KeycloakCreateUserResponse::class.java
    )

    /**
     * Admin API - 사용자 정보 조회
     * JSON 기반 GET 요청, Bearer 토큰 인증이 필요하므로 executeAdminApiCall 사용
     */
    suspend fun getUser(
        adminToken: String,
        userId: String,
    ): KeycloakResponse = executeAdminApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = "GET",
        adminToken = adminToken,
        responseType = KeycloakUserResponse::class.java
    )

    /**
     * Admin API - 사용자 정보 수정
     * JSON 기반 PUT 요청, Bearer 토큰 인증이 필요하므로 executeAdminApiCall 사용
     */
    suspend fun updateUser(
        adminToken: String,
        userId: String,
        request: KeycloakUpdateUserRequest,
    ): KeycloakResponse = executeAdminApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = "PUT",
        adminToken = adminToken,
        request = request,
        responseType = KeycloakUserResponse::class.java
    )

    /**
     * Direct Grant - 로그인
     * form-urlencoded 기반 POST 요청, 토큰 응답이므로 executeTokenApiCall 사용
     */
    suspend fun login(request: KeycloakLoginRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
            add("username", request.username)
            add("password", request.password)
            add("scope", request.scope)
        }
        return executeTokenApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/token", formData)
    }

    /**
     * 토큰 갱신
     * form-urlencoded 기반 POST 요청, 토큰 응답이므로 executeTokenApiCall 사용
     */
    suspend fun refreshToken(request: KeycloakRefreshTokenRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
            add("refresh_token", request.refreshToken)
        }
        return executeTokenApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/token", formData)
    }

    /**
     * 로그아웃
     * form-urlencoded 기반 POST 요청, 빈 응답이므로 executeLogoutApiCall 사용
     */
    suspend fun logout(request: KeycloakLogoutRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("refresh_token", request.refreshToken)
        }
        return executeLogoutApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/logout", formData)
    }

    /**
     * Admin 토큰 획득
     * form-urlencoded 기반 POST 요청, 토큰 응답이므로 executeTokenApiCall 사용
     */
    suspend fun getAdminToken(request: KeycloakAdminTokenRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
        }
        return executeTokenApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/token", formData)
    }

    /**
     * UserInfo 조회
     * GET 요청, Bearer 토큰 헤더 필요, 사용자 정보 응답이므로 executeUserInfoApiCall 사용
     */
    suspend fun getUserInfo(accessToken: String): KeycloakResponse =
        executeUserInfoApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/userinfo", accessToken)

    /**
     * 토큰 발급/갱신 API 호출
     *
     * HTTP Method: POST
     * Content-Type: application/x-www-form-urlencoded
     * 응답 타입: KeycloakTokenResponse (access_token, refresh_token 등)
     * 사용 API: login, refreshToken, getAdminToken
     */
    private suspend fun executeTokenApiCall(
        uri: String,
        formData: LinkedMultiValueMap<String, String>,
    ): KeycloakResponse {
        return try {
            val response = keycloakWebClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .awaitBody<String>()

            try {
                objectMapper.readValue(response, KeycloakTokenResponse::class.java)
            } catch (e: Exception) {
                val errorCode = KeycloakApiErrorCode.JSON_PARSING_ERROR
                KeycloakErrorResponse(
                    error = errorCode.code,
                    errorDescription = "응답 파싱 오류: ${e.message}"
                )
            }
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * 로그아웃 API 호출
     *
     * HTTP Method: POST
     * Content-Type: application/x-www-form-urlencoded
     * 응답: 빈 응답 (204 No Content)
     * 특징: 응답 본문이 없음, awaitBodilessEntity() 사용
     */
    private suspend fun executeLogoutApiCall(
        uri: String,
        formData: LinkedMultiValueMap<String, String>,
    ): KeycloakResponse {
        return try {
            keycloakWebClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .awaitBodilessEntity()

            KeycloakLogoutResponse()
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * 사용자 정보 조회 API 호출
     *
     * HTTP Method: GET
     * Authorization: Bearer 토큰 헤더 필요
     * 응답 타입: KeycloakUserInfoResponse (사용자 정보)
     */
    private suspend fun executeUserInfoApiCall(
        uri: String,
        accessToken: String,
    ): KeycloakResponse {
        return try {
            val response = keycloakWebClient
                .get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .awaitBody<String>()

            try {
                objectMapper.readValue(response, KeycloakUserInfoResponse::class.java)
            } catch (e: Exception) {
                val errorCode = KeycloakApiErrorCode.JSON_PARSING_ERROR
                KeycloakErrorResponse(
                    error = errorCode.code,
                    errorDescription = "응답 파싱 오류: ${e.message}"
                )
            }
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * 관리자 API (사용자 CRUD) 호출
     *
     * Content-Type: application/json
     * HTTP Method: GET, POST, PUT, DELETE 모두 지원
     * Authorization: Bearer 토큰 (Admin 권한)
     * 응답: 다양한 타입 또는 빈 응답
     */
    private suspend fun <T : KeycloakResponse> executeAdminApiCall(
        uri: String,
        method: String,
        adminToken: String,
        request: Any? = null,
        responseType: Class<T>,
    ): KeycloakResponse {
        return try {
            val response = when (method) {
                "GET" -> {
                    keycloakWebClient
                        .get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                        .retrieve()
                        .awaitBody<String>()
                }

                "POST" -> {
                    keycloakWebClient
                        .post()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request ?: "")
                        .retrieve()
                        .awaitBody<String>()
                }

                "PUT" -> {
                    keycloakWebClient
                        .put()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request ?: "")
                        .retrieve()
                        .awaitBody<String>()
                }

                "DELETE" -> {
                    keycloakWebClient
                        .delete()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                        .retrieve()
                        .awaitBodilessEntity()

                    "" // DELETE는 빈 응답
                }

                else -> throw IllegalArgumentException("지원하지 않는 HTTP 메서드: $method")
            }

            // 응답 파싱 시도
            if (response.isBlank()) {
                try {
                    // 빈 응답의 경우 기본 생성자로 객체 생성
                    responseType.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    val errorCode = KeycloakApiErrorCode.JSON_PARSING_ERROR
                    KeycloakErrorResponse(
                        error = errorCode.code,
                        errorDescription = "빈 응답 처리 오류: ${e.message}"
                    )
                }
            } else {
                try {
                    // 성공 응답으로 파싱 시도
                    objectMapper.readValue(response, responseType)
                } catch (e: Exception) {
                    try {
                        // 성공 파싱 실패시 에러 응답으로 파싱 시도
                        objectMapper.readValue(response, KeycloakErrorResponse::class.java)
                    } catch (_: Exception) {
                        val errorCode = KeycloakApiErrorCode.JSON_PARSING_ERROR
                        KeycloakErrorResponse(
                            error = errorCode.code,
                            errorDescription = "응답 파싱 오류: ${e.message}"
                        )
                    }
                }
            }
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * HTTP 에러를 처리하고 적절한 에러 응답을 생성합니다.
     */
    private fun handleHttpError(e: WebClientResponseException): KeycloakErrorResponse {
        return try {
            // Keycloak 표준 에러 응답으로 파싱 시도
            objectMapper.readValue(e.responseBodyAsString, KeycloakErrorResponse::class.java)
        } catch (_: Exception) {
            // 파싱 실패시 HTTP 상태 코드 기반 에러 응답 생성
            val errorCode = KeycloakApiErrorCode.fromStatus(e.statusCode.value())
            KeycloakErrorResponse(
                error = errorCode.code,
                errorDescription = errorCode.message
            )
        }
    }

    /**
     * 일반적인 예외를 처리하고 적절한 에러 응답을 생성합니다.
     */
    private fun handleGenericError(e: Exception): KeycloakErrorResponse {
        val errorCode = when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException -> KeycloakApiErrorCode.TIMEOUT

            is java.net.UnknownHostException -> KeycloakApiErrorCode.CONNECTION_ERROR
            else -> KeycloakApiErrorCode.UNKNOWN
        }

        return KeycloakErrorResponse(
            error = errorCode.code,
            errorDescription = "${errorCode.message}: ${e.message ?: "알 수 없는 오류"}"
        )
    }
}