package kr.pincoin.api.external.s3.api.response

import java.time.LocalDateTime

data class S3FileInfoResponse(
    val fileId: String,
    val fileName: String,
    val originalFileName: String,
    val fileKey: String,
    val fileSize: Long,
    val contentType: String,
    val lastModified: LocalDateTime,
    val eTag: String,
    val metadata: Map<String, String> = emptyMap()
)