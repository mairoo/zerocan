package kr.pincoin.api.external.s3.api.response

data class S3UploadFailure(
    val fileName: String,
    val errorCode: String,
    val errorMessage: String
)