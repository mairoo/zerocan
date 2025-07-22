package kr.pincoin.api.external.s3.api.response

data class S3PresignedUrlResponse(
    val url: String,
    val fileKey: String,
    val expiresIn: Long, // 초 단위
    val expiresAt: String, // ISO 8601 형식
    val fields: Map<String, String> = emptyMap() // POST 업로드용 필드들
)