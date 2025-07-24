package kr.pincoin.api.external.s3.api.response

data class S3CheckResult(
    val checkName: String,
    val success: Boolean,
    val message: String,
    val duration: Long? = null,
    val errorCode: String? = null
)