package kr.pincoin.api.external.s3.api.response

data class S3FileListResponse(
    val files: List<S3FileInfoResponse>,
    val totalCount: Long,
    val hasMore: Boolean,
    val nextToken: String? = null // 페이징용 토큰
)