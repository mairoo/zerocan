package kr.pincoin.api.external.auth.keycloak.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kr.pincoin.api.external.auth.keycloak.api.request.*
import kr.pincoin.api.external.auth.keycloak.api.response.*
import kr.pincoin.api.external.auth.keycloak.error.KeycloakApiErrorCode
import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
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
     */
    suspend fun createUser(
        adminToken: String,
        request: KeycloakCreateUserRequest,
    ): KeycloakResponse = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users",
        method = HttpMethod.POST,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        contentType = MediaType.APPLICATION_JSON,
        request = request,
        responseType = KeycloakCreateUserResponse::class.java,
        isLocationResponse = true
    )

    /**
     * Admin API - 사용자 삭제
     */
    suspend fun deleteUser(
        adminToken: String,
        userId: String
    ): KeycloakResponse = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.DELETE,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        responseType = KeycloakDeleteUserResponse::class.java,
    )

    /**
     * Admin API - 사용자 정보 조회
     */
    suspend fun getUser(
        adminToken: String,
        userId: String,
    ): KeycloakResponse = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.GET,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        responseType = KeycloakUserResponse::class.java,
    )

    /**
     * Admin API - 사용자 정보 수정
     */
    suspend fun updateUser(
        adminToken: String,
        userId: String,
        request: KeycloakUpdateUserRequest,
    ): KeycloakResponse = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.PUT,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        contentType = MediaType.APPLICATION_JSON,
        request = request,
        responseType = KeycloakUserResponse::class.java,
    )

    /**
     * Direct Grant - 로그인
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

        return executeApiCall(
            uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            method = HttpMethod.POST,
            contentType = MediaType.APPLICATION_FORM_URLENCODED,
            formData = formData,
            responseType = KeycloakTokenResponse::class.java,
        )
    }

    /**
     * 토큰 갱신
     */
    suspend fun refreshToken(request: KeycloakRefreshTokenRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
            add("refresh_token", request.refreshToken)
        }

        return executeApiCall(
            uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            method = HttpMethod.POST,
            contentType = MediaType.APPLICATION_FORM_URLENCODED,
            formData = formData,
            responseType = KeycloakTokenResponse::class.java,
        )
    }

    /**
     * 로그아웃
     */
    suspend fun logout(request: KeycloakLogoutRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("refresh_token", request.refreshToken)
        }

        return executeApiCall(
            uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/logout",
            method = HttpMethod.POST,
            contentType = MediaType.APPLICATION_FORM_URLENCODED,
            formData = formData,
            responseType = KeycloakLogoutResponse::class.java,
        )
    }

    /**
     * Admin 토큰 획득
     */
    suspend fun getAdminToken(request: KeycloakAdminTokenRequest): KeycloakResponse {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
        }

        return executeApiCall(
            uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            method = HttpMethod.POST,
            contentType = MediaType.APPLICATION_FORM_URLENCODED,
            formData = formData,
            responseType = KeycloakTokenResponse::class.java,
        )
    }

    /**
     * UserInfo 조회
     */
    suspend fun getUserInfo(accessToken: String): KeycloakResponse = executeApiCall(
        uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/userinfo",
        method = HttpMethod.GET,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $accessToken"),
        responseType = KeycloakUserInfoResponse::class.java,
    )

    /**
     * HTTP API 요청을 실행하고 응답을 처리하는 통합 메서드입니다.
     *
     * 처리 흐름:
     * 1. HTTP 요청 실행 (GET, POST, PUT, DELETE 지원)
     * 2. 응답 타입에 따른 파싱 처리
     * 3. Location 헤더 처리 (사용자 생성 시)
     * 4. HTTP 통신/파싱 실패시 적절한 에러 응답 생성
     *
     * @param uri API 엔드포인트 URI
     * @param method HTTP 메서드
     * @param headers HTTP 헤더
     * @param contentType Content-Type
     * @param request 요청 본문 객체 (JSON)
     * @param formData 폼 데이터 (form-urlencoded)
     * @param responseType 성공 응답을 파싱할 클래스 타입
     * @param isLocationResponse Location 헤더에서 ID를 추출해야 하는지 여부
     * @return 성공시 지정된 응답 타입(T), 실패시 KeycloakErrorResponse
     */
    private suspend fun <T : KeycloakResponse> executeApiCall(
        uri: String,
        method: HttpMethod,
        headers: Map<String, String> = emptyMap(),
        contentType: MediaType = MediaType.APPLICATION_JSON,
        request: Any? = null,
        formData: LinkedMultiValueMap<String, String>? = null,
        responseType: Class<T>,
        isLocationResponse: Boolean = false,
    ): KeycloakResponse =
        try {
            // 1. HTTP 요청 실행
            val webClientSpec = when (method) {
                HttpMethod.GET -> keycloakWebClient.get()
                HttpMethod.POST -> keycloakWebClient.post()
                HttpMethod.PUT -> keycloakWebClient.put()
                HttpMethod.DELETE -> keycloakWebClient.delete()
                else -> throw IllegalArgumentException("지원하지 않는 HTTP 메서드: $method")
            }

            // 공통 헤더 설정
            var requestSpec = webClientSpec.uri(uri)
            headers.forEach { (key, value) ->
                requestSpec = requestSpec.header(key, value)
            }

            // Content-Type 및 Body 설정은 HTTP 메서드별로 처리

            // Content-Type 및 Body 설정, 그리고 응답 처리
            when {
                isLocationResponse -> {
                    // Location 헤더에서 사용자 ID 추출 (POST with body)
                    val response = (requestSpec as WebClient.RequestBodyUriSpec)
                        .contentType(contentType)
                        .bodyValue(request ?: "")
                        .retrieve()
                        .toBodilessEntity()
                        .awaitSingle()

                    val locationHeader = response.headers.getFirst(HttpHeaders.LOCATION)
                    val userId = extractUserIdFromLocation(locationHeader)

                    if (userId != null) {
                        KeycloakCreateUserResponse(userId = userId)
                    } else {
                        val errorCode = KeycloakApiErrorCode.JSON_PARSING_ERROR
                        KeycloakErrorResponse(
                            error = errorCode.code,
                            errorDescription = "Location 헤더에서 사용자 ID를 추출할 수 없습니다."
                        )
                    }
                }

                method == HttpMethod.DELETE -> {
                    // DELETE는 빈 응답
                    requestSpec.retrieve().awaitBodilessEntity()
                    responseType.getDeclaredConstructor().newInstance()
                }

                method == HttpMethod.GET -> {
                    // GET 요청 - body 없음
                    val response = requestSpec.retrieve().awaitBody<String>()

                    if (response.isBlank()) {
                        responseType.getDeclaredConstructor().newInstance()
                    } else {
                        try {
                            objectMapper.readValue(response, responseType)
                        } catch (e: Exception) {
                            try {
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
                }

                else -> {
                    // POST, PUT 요청 - body 있음
                    val bodySpec = (requestSpec as WebClient.RequestBodyUriSpec)
                        .contentType(contentType)

                    val responseSpec = when {
                        formData != null -> bodySpec.body(BodyInserters.fromFormData(formData))
                        request != null -> bodySpec.bodyValue(request)
                        else -> bodySpec.bodyValue("")
                    }

                    val response = responseSpec.retrieve().awaitBody<String>()

                    if (response.isBlank()) {
                        responseType.getDeclaredConstructor().newInstance()
                    } else {
                        try {
                            objectMapper.readValue(response, responseType)
                        } catch (e: Exception) {
                            try {
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
                }
            }
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }

    /**
     * Location 헤더에서 사용자 ID 추출
     */
    private fun extractUserIdFromLocation(
        locationHeader: String?,
    ): String? =
        locationHeader?.let { location ->
            location.substringAfterLast("/")
                .takeIf { it.isNotBlank() && it.matches(UUID_REGEX) }
        }

    /**
     * HTTP 에러를 처리하고 적절한 에러 응답을 생성합니다.
     */
    private fun handleHttpError(
        e: WebClientResponseException,
    ): KeycloakErrorResponse =
        try {
            objectMapper.readValue(e.responseBodyAsString, KeycloakErrorResponse::class.java)
        } catch (_: Exception) {
            val errorCode = KeycloakApiErrorCode.fromStatus(e.statusCode.value())
            KeycloakErrorResponse(
                error = errorCode.code,
                errorDescription = "${errorCode.message}: ${e.statusText}"
            )
        }

    /**
     * 일반적인 예외를 처리하고 적절한 에러 응답을 생성합니다.
     */
    private fun handleGenericError(
        e: Exception,
    ): KeycloakErrorResponse {
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

    companion object {
        private val UUID_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )
    }
}