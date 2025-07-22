package kr.pincoin.api.external.s3.api.response

sealed class S3ApiResponse<out T> {
    data class Success<T>(val data: T) : S3ApiResponse<T>()

    data class Error<T>(
        val errorCode: String,
        val errorMessage: String
    ) : S3ApiResponse<T>()
}