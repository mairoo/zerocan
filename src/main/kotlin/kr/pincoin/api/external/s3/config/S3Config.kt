package kr.pincoin.api.external.s3.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.external.s3.properties.S3Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

@Configuration
class S3Config(
    private val s3Properties: S3Properties,
) {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun s3Client(): S3Client {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
        )

        val builder = S3Client.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(credentialsProvider)

        s3Properties.endpoint?.let { endpoint ->
            logger.info { "Using custom S3 endpoint: $endpoint" }
            builder.endpointOverride(URI.create(endpoint))
            builder.forcePathStyle(true)
        }

        return builder.build()
    }

    @Bean
    fun s3AsyncClient(): S3AsyncClient {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
        )

        val httpClient = NettyNioAsyncHttpClient.builder()
            .connectionTimeout(Duration.ofMillis(s3Properties.timeout))
            .connectionAcquisitionTimeout(Duration.ofMillis(s3Properties.timeout))
            .maxConcurrency(50)
            .build()

        val builder = S3AsyncClient.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient)

        s3Properties.endpoint?.let { endpoint ->
            logger.info { "Using custom S3 endpoint (async): $endpoint" }
            builder.endpointOverride(URI.create(endpoint))
            builder.forcePathStyle(true)
        }

        return builder.build()
    }

    @Bean
    fun s3Presigner(): S3Presigner {
        val credentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
        )

        val builder = S3Presigner.builder()
            .region(Region.of(s3Properties.region))
            .credentialsProvider(credentialsProvider)

        s3Properties.endpoint?.let { endpoint ->
            logger.info { "Using custom S3 endpoint (presigner): $endpoint" }
            builder.endpointOverride(URI.create(endpoint))
        }

        return builder.build()
    }
}