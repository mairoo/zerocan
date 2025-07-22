package kr.pincoin.api.external.s3.api.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class S3PresignedUrlRequest(
    @field:NotBlank(message = "파일명은 필수입니다")
    val fileName: String,

    @field:NotBlank(message = "파일 타입은 필수입니다")
    val contentType: String,

    val filePath: String? = null, // 선택적 경로 지정

    @field:Min(value = 60, message = "최소 1분 이상이어야 합니다")
    @field:Max(value = 604800, message = "최대 7일까지 가능합니다")
    val expirationSeconds: Long = 3600, // 기본 1시간

    val metadata: Map<String, String> = emptyMap()
)