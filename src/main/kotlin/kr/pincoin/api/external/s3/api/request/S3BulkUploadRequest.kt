package kr.pincoin.api.external.s3.api.request

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

data class S3BulkUploadRequest(
    @field:NotEmpty(message = "업로드할 파일이 없습니다")
    @field:Size(max = 10, message = "최대 10개 파일까지 업로드 가능합니다")
    val files: List<MultipartFile>,

    val basePath: String? = null, // 공통 기본 경로
    val metadata: Map<String, String> = emptyMap()
)