package kr.pincoin.api.app.auth.controller

import jakarta.validation.Valid
import kr.pincoin.api.app.auth.request.RecaptchaV2VerifyRequest
import kr.pincoin.api.app.auth.request.RecaptchaV3VerifyRequest
import kr.pincoin.api.app.auth.response.RecaptchaStatusResponse
import kr.pincoin.api.app.auth.response.RecaptchaTestResponse
import kr.pincoin.api.external.auth.recaptcha.api.response.RecaptchaResponse
import kr.pincoin.api.external.auth.recaptcha.error.RecaptchaErrorCode
import kr.pincoin.api.external.auth.recaptcha.service.RecaptchaService
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/open/recaptcha")
class RecaptchaController(
    private val recaptchaService: RecaptchaService,
) {

    /**
     * reCAPTCHA v2 검증 테스트
     */
    @PostMapping("/v2/verify")
    suspend fun verifyV2(
        @Valid @RequestBody request: RecaptchaV2VerifyRequest
    ): ResponseEntity<RecaptchaTestResponse> {
        val result = recaptchaService.verifyV2(request.token)

        return when (result) {
            is RecaptchaResponse.Success -> ResponseEntity.ok(
                RecaptchaTestResponse(
                    success = true,
                    message = "reCAPTCHA v2 검증 성공",
                    data = result.data,
                )
            )

            is RecaptchaResponse.Error -> throw BusinessException(
                mapErrorCodeToRecaptchaErrorCode(result.errorCode)
            )
        }
    }

    /**
     * reCAPTCHA v3 검증 테스트
     */
    @PostMapping("/v3/verify")
    suspend fun verifyV3(
        @Valid @RequestBody request: RecaptchaV3VerifyRequest
    ): ResponseEntity<RecaptchaTestResponse> {
        val result = recaptchaService.verifyV3(request.token, request.minScore)

        return when (result) {
            is RecaptchaResponse.Success -> ResponseEntity.ok(
                RecaptchaTestResponse(
                    success = true,
                    message = "reCAPTCHA v3 검증 성공 (점수: ${result.data.score})",
                    data = result.data,
                )
            )

            is RecaptchaResponse.Error -> throw BusinessException(
                mapErrorCodeToRecaptchaErrorCode(result.errorCode)
            )
        }
    }

    /**
     * reCAPTCHA 상태 확인
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<RecaptchaStatusResponse> {
        return ResponseEntity.ok(
            RecaptchaStatusResponse(
                enabled = true, // RecaptchaProperties에서 가져올 수 있지만 테스트용으로 하드코딩
                message = "reCAPTCHA 서비스가 활성화되어 있습니다"
            )
        )
    }

    /**
     * 에러 코드 매핑
     */
    private fun mapErrorCodeToRecaptchaErrorCode(errorCode: String): RecaptchaErrorCode {
        return when (errorCode) {
            "VERIFICATION_FAILED" -> RecaptchaErrorCode.VERIFICATION_FAILED
            "INVALID_TOKEN" -> RecaptchaErrorCode.INVALID_TOKEN
            "TIMEOUT", "TIMEOUT_OR_DUPLICATE" -> RecaptchaErrorCode.TIMEOUT_OR_DUPLICATE
            "HOSTNAME_MISMATCH" -> RecaptchaErrorCode.HOSTNAME_MISMATCH
            "LOW_SCORE" -> RecaptchaErrorCode.LOW_SCORE
            "NETWORK_ERROR", "CONNECTION_ERROR", "HTTP_ERROR" -> RecaptchaErrorCode.NETWORK_ERROR
            "SYSTEM_ERROR", "PARSE_ERROR" -> RecaptchaErrorCode.SYSTEM_ERROR
            else -> RecaptchaErrorCode.UNKNOWN
        }
    }
}