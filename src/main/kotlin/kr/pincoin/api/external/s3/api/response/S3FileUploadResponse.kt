package kr.pincoin.api.external.s3.api.response

import java.time.LocalDateTime

data class S3FileUploadResponse(
    val fileId: String,
    val fileName: String,
    val originalFileName: String,
    val fileUrl: String,
    val fileKey: String, // S3 Object Key
    val fileSize: Long,
    val contentType: String,
    val uploadedAt: LocalDateTime,
    val eTag: String? = null,
    val versionId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)