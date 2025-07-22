package kr.pincoin.api.external.s3.config

import kr.pincoin.api.external.s3.properties.S3Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

@Configuration
class S3Config(
    private val s3Properties: S3Properties,
) {
    @Bean
    fun s3Client(): S3Client {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                s3Properties.accessKey,
                s3Properties.secretKey
            )
        )

        val httpClient = ApacheHttpClient.builder()
            .connectionTimeout(Duration.ofMillis(s3Properties.timeout))
            .socketTimeout(Duration.ofMillis(s3Properties.timeout))
            .build()

        return S3Client.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient)
            .apply {
                // LocalStack이나 MinIO 등 커스텀 엔드포인트 설정
                s3Properties.endpoint?.let { endpoint ->
                    endpointOverride(URI.create(endpoint))
                    forcePathStyle(true) // 커스텀 엔드포인트에서는 Path Style 사용
                }
            }
            .build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                s3Properties.accessKey,
                s3Properties.secretKey
            )
        )

        return S3Presigner.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(credentialsProvider)
            .apply {
                // 커스텀 엔드포인트 설정
                s3Properties.endpoint?.let { endpoint ->
                    endpointOverride(URI.create(endpoint))
                }
            }
            .build()
    }
}