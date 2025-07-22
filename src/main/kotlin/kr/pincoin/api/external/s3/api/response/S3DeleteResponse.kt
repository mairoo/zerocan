package kr.pincoin.api.external.s3.api.response

import java.time.LocalDateTime

data class S3DeleteResponse(
    val fileId: String,
    val fileKey: String,
    val deleted: Boolean,
    val deletedAt: LocalDateTime? = null
)