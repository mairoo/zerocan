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
    ): KeycloakResponse<KeycloakCreateUserData> = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users",
        method = HttpMethod.POST,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        contentType = MediaType.APPLICATION_JSON,
        request = request,
        isLocationResponse = true
    ) { locationHeader ->
        val userId = extractUserIdFromLocation(locationHeader)
        if (userId != null) {
            KeycloakResponse.Success(KeycloakCreateUserData(userId = userId))
        } else {
            KeycloakResponse.Error("PARSE_ERROR", "Location 헤더에서 사용자 ID를 추출할 수 없습니다")
        }
    }

    /**
     * Admin API - 사용자 삭제
     */
    suspend fun deleteUser(
        adminToken: String,
        userId: String
    ): KeycloakResponse<KeycloakLogoutData> = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.DELETE,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken")
    ) {
        KeycloakResponse.Success(KeycloakLogoutData) // 삭제는 성공만 중요
    }

    /**
     * Admin API - 사용자 정보 조회
     */
    suspend fun getUser(
        adminToken: String,
        userId: String,
    ): KeycloakResponse<KeycloakUserData> = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.GET,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken")
    ) { responseBody ->
        parseUserData(responseBody)
    }

    /**
     * Admin API - 사용자 정보 수정
     */
    suspend fun updateUser(
        adminToken: String,
        userId: String,
        request: KeycloakUpdateUserRequest,
    ): KeycloakResponse<KeycloakUserData> = executeApiCall(
        uri = "/admin/realms/${keycloakProperties.realm}/users/$userId",
        method = HttpMethod.PUT,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $adminToken"),
        contentType = MediaType.APPLICATION_JSON,
        request = request
    ) { responseBody ->
        parseUserData(responseBody)
    }

    /**
     * Direct Grant - 로그인
     */
    suspend fun login(request: KeycloakLoginRequest): KeycloakResponse<KeycloakTokenData> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
            add("username", request.username)
            add("password", request.password)
            add("scope", request.scope)
        }

        return executeTokenApiCall(
            "/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            formData
        )
    }

    /**
     * 토큰 갱신
     */
    suspend fun refreshToken(request: KeycloakRefreshTokenRequest): KeycloakResponse<KeycloakTokenData> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
            add("refresh_token", request.refreshToken)
        }

        return executeTokenApiCall(
            "/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            formData
        )
    }

    /**
     * 로그아웃
     */
    suspend fun logout(request: KeycloakLogoutRequest): KeycloakResponse<KeycloakLogoutData> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("refresh_token", request.refreshToken)
        }

        return executeLogoutApiCall(
            "/realms/${keycloakProperties.realm}/protocol/openid-connect/logout",
            formData
        )
    }

    /**
     * Admin 토큰 획득
     */
    suspend fun getAdminToken(request: KeycloakAdminTokenRequest): KeycloakResponse<KeycloakAdminTokenData> {
        val formData = LinkedMultiValueMap<String, String>().apply {
            add("client_id", request.clientId)
            add("client_secret", request.clientSecret)
            add("grant_type", request.grantType)
        }

        return when (val tokenResult =
            executeTokenApiCall("/realms/${keycloakProperties.realm}/protocol/openid-connect/token", formData)) {
            is KeycloakResponse.Success -> KeycloakResponse.Success(
                KeycloakAdminTokenData(accessToken = tokenResult.data.accessToken)
            )

            is KeycloakResponse.Error -> KeycloakResponse.Error(
                errorCode = tokenResult.errorCode,
                errorMessage = tokenResult.errorMessage
            )
        }
    }

    /**
     * UserInfo 조회
     */
    suspend fun getUserInfo(accessToken: String): KeycloakResponse<KeycloakUserInfoData> = executeApiCall(
        uri = "/realms/${keycloakProperties.realm}/protocol/openid-connect/userinfo",
        method = HttpMethod.GET,
        headers = mapOf(HttpHeaders.AUTHORIZATION to "Bearer $accessToken")
    ) { responseBody ->
        parseUserInfoData(responseBody)
    }

    /**
     * 통합된 API 호출 메서드
     */
    private suspend fun <T> executeApiCall(
        uri: String,
        method: HttpMethod,
        headers: Map<String, String> = emptyMap(),
        contentType: MediaType = MediaType.APPLICATION_JSON,
        request: Any? = null,
        isLocationResponse: Boolean = false,
        responseParser: (String) -> KeycloakResponse<T>
    ): KeycloakResponse<T> {
        return try {
            val webClientSpec = when (method) {
                HttpMethod.GET -> keycloakWebClient.get()
                HttpMethod.POST -> keycloakWebClient.post()
                HttpMethod.PUT -> keycloakWebClient.put()
                HttpMethod.DELETE -> keycloakWebClient.delete()
                else -> throw IllegalArgumentException("지원하지 않는 HTTP 메서드: $method")
            }

            var requestSpec = webClientSpec.uri(uri)
            headers.forEach { (key, value) ->
                requestSpec = requestSpec.header(key, value)
            }

            when {
                isLocationResponse -> {
                    val response = (requestSpec as WebClient.RequestBodyUriSpec)
                        .contentType(contentType)
                        .bodyValue(request ?: "")
                        .retrieve()
                        .toBodilessEntity()
                        .awaitSingle()

                    val locationHeader = response.headers.getFirst(HttpHeaders.LOCATION)
                    responseParser(locationHeader ?: "")
                }

                method == HttpMethod.DELETE -> {
                    requestSpec.retrieve().awaitBodilessEntity()
                    responseParser("")
                }

                method == HttpMethod.GET -> {
                    val response = requestSpec.retrieve().awaitBody<String>()
                    responseParser(response)
                }

                else -> {
                    val response = (requestSpec as WebClient.RequestBodyUriSpec)
                        .contentType(contentType)
                        .bodyValue(request ?: "")
                        .retrieve()
                        .awaitBody<String>()

                    responseParser(response)
                }
            }
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * 토큰 API 호출 (form-urlencoded)
     */
    private suspend fun executeTokenApiCall(
        uri: String,
        formData: LinkedMultiValueMap<String, String>
    ): KeycloakResponse<KeycloakTokenData> {
        return try {
            val response = keycloakWebClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .awaitBody<String>()

            parseTokenData(response)
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    /**
     * 로그아웃 API 호출
     */
    private suspend fun executeLogoutApiCall(
        uri: String,
        formData: LinkedMultiValueMap<String, String>
    ): KeycloakResponse<KeycloakLogoutData> {
        return try {
            keycloakWebClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .awaitBodilessEntity()

            KeycloakResponse.Success(KeycloakLogoutData)
        } catch (e: WebClientResponseException) {
            handleHttpError(e)
        } catch (e: Exception) {
            handleGenericError(e)
        }
    }

    // JSON 파싱 메서드들
    private fun parseTokenData(response: String): KeycloakResponse<KeycloakTokenData> {
        return try {
            val jsonNode = objectMapper.readTree(response)

            // 에러 체크
            if (jsonNode.has("error")) {
                return KeycloakResponse.Error(
                    errorCode = jsonNode.get("error").asText(),
                    errorMessage = jsonNode.get("error_description")?.asText() ?: "토큰 요청 실패"
                )
            }

            KeycloakResponse.Success(
                KeycloakTokenData(
                    accessToken = jsonNode.get("access_token").asText(),
                    expiresIn = jsonNode.get("expires_in").asLong(),
                    refreshExpiresIn = jsonNode.get("refresh_expires_in").asLong(),
                    refreshToken = jsonNode.get("refresh_token")?.asText(),
                    tokenType = jsonNode.get("token_type").asText(),
                    idToken = jsonNode.get("id_token")?.asText(),
                    sessionState = jsonNode.get("session_state")?.asText(),
                    scope = jsonNode.get("scope")?.asText(),
                )
            )
        } catch (e: Exception) {
            KeycloakResponse.Error("PARSE_ERROR", "토큰 응답 파싱 실패: ${e.message}")
        }
    }

    private fun parseUserData(response: String): KeycloakResponse<KeycloakUserData> {
        return try {
            val jsonNode = objectMapper.readTree(response)

            KeycloakResponse.Success(
                KeycloakUserData(
                    id = jsonNode.get("id").asText(),
                    username = jsonNode.get("username").asText(),
                    enabled = jsonNode.get("enabled").asBoolean(),
                    emailVerified = jsonNode.get("emailVerified").asBoolean(),
                    firstName = jsonNode.get("firstName")?.asText(),
                    lastName = jsonNode.get("lastName")?.asText(),
                    email = jsonNode.get("email")?.asText(),
                    createdTimestamp = jsonNode.get("createdTimestamp")?.asLong(),
                )
            )
        } catch (e: Exception) {
            KeycloakResponse.Error("PARSE_ERROR", "사용자 데이터 파싱 실패: ${e.message}")
        }
    }

    private fun parseUserInfoData(response: String): KeycloakResponse<KeycloakUserInfoData> {
        return try {
            val jsonNode = objectMapper.readTree(response)

            KeycloakResponse.Success(
                KeycloakUserInfoData(
                    sub = jsonNode.get("sub").asText(),
                    emailVerified = jsonNode.get("email_verified").asBoolean(),
                    preferredUsername = jsonNode.get("preferred_username").asText(),
                    name = jsonNode.get("name")?.asText(),
                    givenName = jsonNode.get("given_name")?.asText(),
                    familyName = jsonNode.get("family_name")?.asText(),
                    email = jsonNode.get("email")?.asText(),
                )
            )
        } catch (e: Exception) {
            KeycloakResponse.Error("PARSE_ERROR", "사용자 정보 파싱 실패: ${e.message}")
        }
    }

    private fun extractUserIdFromLocation(locationHeader: String?): String? {
        return locationHeader?.let { location ->
            location.substringAfterLast("/")
                .takeIf { it.isNotBlank() && it.matches(UUID_REGEX) }
        }
    }

    private fun handleHttpError(e: WebClientResponseException): KeycloakResponse<Nothing> {
        return try {
            val jsonNode = objectMapper.readTree(e.responseBodyAsString)
            if (jsonNode.has("error")) {
                KeycloakResponse.Error(
                    errorCode = jsonNode.get("error").asText(),
                    errorMessage = jsonNode.get("error_description")?.asText() ?: "HTTP 오류"
                )
            } else {
                val errorCode = KeycloakApiErrorCode.fromStatus(e.statusCode.value())
                KeycloakResponse.Error(
                    errorCode = errorCode.code,
                    errorMessage = errorCode.message
                )
            }
        } catch (_: Exception) {
            val errorCode = KeycloakApiErrorCode.fromStatus(e.statusCode.value())
            KeycloakResponse.Error(
                errorCode = errorCode.code,
                errorMessage = "${errorCode.message}: ${e.statusText}"
            )
        }
    }

    private fun handleGenericError(e: Exception): KeycloakResponse<Nothing> {
        val errorCode = when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException -> KeycloakApiErrorCode.TIMEOUT

            is java.net.UnknownHostException -> KeycloakApiErrorCode.CONNECTION_ERROR
            else -> KeycloakApiErrorCode.UNKNOWN
        }

        return KeycloakResponse.Error(
            errorCode = errorCode.code,
            errorMessage = "${errorCode.message}: ${e.message ?: "알 수 없는 오류"}"
        )
    }

    companion object {
        private val UUID_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        )
    }
}