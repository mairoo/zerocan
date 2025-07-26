package kr.pincoin.api.external.auth.recaptcha.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.auth.recaptcha.api.request.RecaptchaVerifyRequest
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaResponse
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaVerifyData
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaVerifyResponse
import kr.pincoin.api.external.auth.recaptcha.properties.RecaptchaProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody

@Component
class RecaptchaApiClient(
    private val recaptchaWebClient: WebClient,
    private val recaptchaProperties: RecaptchaProperties,
    private val objectMapper: ObjectMapper,
) {
    /**
     * reCAPTCHA 토큰 검증
     */
    suspend fun verifyToken(request: RecaptchaVerifyRequest): RecaptchaResponse<RecaptchaVerifyData> =
        try {
            val formData = LinkedMultiValueMap<String, String>().apply {
                add("secret", recaptchaProperties.secretKey)
                add("response", request.token)
            }

            val response = recaptchaWebClient
                .post()
                .uri("/recaptcha/api/siteverify")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .awaitBody<String>()

            parseResponse(response)
        } catch (e: WebClientResponseException) {
            RecaptchaResponse.Error("HTTP_ERROR", "HTTP 오류: ${e.statusText}")
        } catch (e: Exception) {
            val errorCode = when (e) {
                is java.net.SocketTimeoutException, is java.net.ConnectException -> "TIMEOUT"
                is java.net.UnknownHostException -> "CONNECTION_ERROR"
                else -> "NETWORK_ERROR"
            }
            RecaptchaResponse.Error(errorCode, "reCAPTCHA 서버 오류: ${e.message}")
        }

    /**
     * 응답 파싱
     */
    private fun parseResponse(response: String): RecaptchaResponse<RecaptchaVerifyData> =
        try {
            val recaptchaResponse = objectMapper.readValue(response, RecaptchaVerifyResponse::class.java)

            val data = RecaptchaVerifyData(
                success = recaptchaResponse.success,
                score = recaptchaResponse.score,
                action = recaptchaResponse.action,
                hostname = recaptchaResponse.hostname,
                challengeTs = recaptchaResponse.challengeTs,
                errorCodes = recaptchaResponse.errorCodes
            )

            RecaptchaResponse.Success(data)
        } catch (e: Exception) {
            RecaptchaResponse.Error("PARSE_ERROR", "응답 파싱 실패: ${e.message}")
        }
}