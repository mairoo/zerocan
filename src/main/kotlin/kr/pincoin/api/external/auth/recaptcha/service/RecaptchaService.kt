package kr.pincoin.api.external.auth.recaptcha.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.auth.recaptcha.api.request.RecaptchaVerifyRequest
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaResponse
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaVerifyData
import kr.pincoin.api.external.auth.recaptcha.properties.RecaptchaProperties
import org.springframework.stereotype.Service

@Service
class RecaptchaService(
    private val recaptchaApiClient: RecaptchaApiClient,
    private val recaptchaProperties: RecaptchaProperties,
) {
    /**
     * reCAPTCHA v2 검증
     */
    suspend fun verifyV2(token: String): RecaptchaResponse<RecaptchaVerifyData> =
        withContext(Dispatchers.IO) {
            // enabled가 false인 경우 무조건 성공 반환
            if (!recaptchaProperties.enabled) {
                return@withContext RecaptchaResponse.Success(
                    RecaptchaVerifyData(
                        success = true,
                        hostname = null,
                        challengeTs = null,
                        errorCodes = null
                    )
                )
            }

            try {
                withTimeout(recaptchaProperties.timeout) {
                    val request = RecaptchaVerifyRequest(token = token)
                    val result = recaptchaApiClient.verifyToken(request)

                    when (result) {
                        is RecaptchaResponse.Success -> validateResponse(result.data)
                        is RecaptchaResponse.Error -> result
                    }
                }
            } catch (_: TimeoutCancellationException) {
                RecaptchaResponse.Error("TIMEOUT", "reCAPTCHA v2 검증 요청 시간 초과")
            } catch (e: Exception) {
                RecaptchaResponse.Error("SYSTEM_ERROR", "reCAPTCHA v2 검증 중 오류: ${e.message}")
            }
        }

    /**
     * reCAPTCHA v3 검증
     */
    suspend fun verifyV3(
        token: String,
        minScore: Double? = null,
    ): RecaptchaResponse<RecaptchaVerifyData> =
        withContext(Dispatchers.IO) {
            // enabled가 false인 경우 무조건 성공 반환
            if (!recaptchaProperties.enabled) {
                return@withContext RecaptchaResponse.Success(
                    RecaptchaVerifyData(
                        success = true,
                        score = 1.0, // v3의 경우 최고 점수로 설정
                        action = null,
                        hostname = null,
                        challengeTs = null,
                        errorCodes = null
                    )
                )
            }

            try {
                withTimeout(recaptchaProperties.timeout) {
                    val request = RecaptchaVerifyRequest(token = token)
                    val result = recaptchaApiClient.verifyToken(request)

                    when (result) {
                        is RecaptchaResponse.Success -> validateResponse(result.data, minScore)
                        is RecaptchaResponse.Error -> result
                    }
                }
            } catch (_: TimeoutCancellationException) {
                RecaptchaResponse.Error("TIMEOUT", "reCAPTCHA v3 검증 요청 시간 초과")
            } catch (e: Exception) {
                RecaptchaResponse.Error("SYSTEM_ERROR", "reCAPTCHA v3 검증 중 오류: ${e.message}")
            }
        }

    /**
     * 응답 검증 로직
     */
    private fun validateResponse(
        data: RecaptchaVerifyData,
        minScore: Double? = null,
    ): RecaptchaResponse<RecaptchaVerifyData> {
        // 1. 성공 여부 확인 (가장 중요)
        if (!data.success) {
            return RecaptchaResponse.Error(
                "VERIFICATION_FAILED",
                mapErrorCodes(data.errorCodes)
            )
        }

        // 2. 점수 검증 (v3인 경우만)
        if (data.score != null) {
            val scoreThreshold = minScore ?: recaptchaProperties.minScore
            if (data.score < scoreThreshold) {
                return RecaptchaResponse.Error(
                    "LOW_SCORE",
                    "점수 부족: ${data.score} < $scoreThreshold",
                )
            }
        }

        return RecaptchaResponse.Success(data)
    }

    /**
     * 에러 코드 매핑
     */
    private fun mapErrorCodes(errorCodes: List<String>?): String {
        if (errorCodes.isNullOrEmpty()) {
            return "reCAPTCHA 검증 실패: 알 수 없는 오류"
        }

        return when {
            errorCodes.contains("missing-input-secret") -> "Secret key가 누락되었습니다"
            errorCodes.contains("invalid-input-secret") -> "유효하지 않은 Secret key입니다"
            errorCodes.contains("missing-input-response") -> "reCAPTCHA 응답이 누락되었습니다"
            errorCodes.contains("invalid-input-response") -> "유효하지 않은 reCAPTCHA 응답입니다"
            errorCodes.contains("bad-request") -> "잘못된 요청입니다"
            errorCodes.contains("timeout-or-duplicate") -> "토큰이 만료되었거나 이미 사용되었습니다"
            else -> "reCAPTCHA 검증 실패: ${errorCodes.joinToString(", ")}"
        }
    }
}