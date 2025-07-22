package kr.pincoin.api.external.s3.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

data class S3FileUploadRequest(
    @field:NotNull(message = "파일은 필수입니다")
    val file: MultipartFile,

    @field:NotBlank(message = "파일 경로는 필수입니다")
    val filePath: String? = null, // 선택적 경로 지정

    val metadata: Map<String, String> = emptyMap() // 추가 메타데이터
)