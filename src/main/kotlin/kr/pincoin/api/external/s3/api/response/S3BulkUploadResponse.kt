package kr.pincoin.api.external.s3.api.response

data class S3BulkUploadResponse(
    val successFiles: List<S3FileUploadResponse>,
    val failedFiles: List<S3UploadFailure>,
    val totalCount: Int,
    val successCount: Int,
    val failureCount: Int
)