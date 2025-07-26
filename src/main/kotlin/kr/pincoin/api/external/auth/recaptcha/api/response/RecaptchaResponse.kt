package kr.pincoin.api.external.auth.recaptcha.api.response

/**
 * reCAPTCHA API 응답의 공통 결과 타입
 */
sealed class RecaptchaResponse<out T> {
    /**
     * 성공 응답
     */
    data class Success<T>(val data: T) : RecaptchaResponse<T>()

    /**
     * 실패 응답
     */
    data class Error<T>(
        val errorCode: String,
        val errorMessage: String
    ) : RecaptchaResponse<T>()
}