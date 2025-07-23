package kr.pincoin.api.external.s3.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    val region: String = "ap-northeast-2",
    val bucketName: String = "dummy-bucket",
    val accessKey: String = "dummy-access-key",
    val secretKey: String = "dummy-secret-key",
    val endpoint: String? = null, // LocalStack 등을 위한 커스텀 엔드포인트
    val timeout: Long = 30000,
    val maxFileSize: Long = 10485760, // 10MB
    val allowedExtensions: List<String> = listOf("jpg", "jpeg", "png", "pdf", "doc", "docx"),
)