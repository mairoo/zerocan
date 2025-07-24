package kr.pincoin.api.external.s3.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.s3.error.S3ErrorCode
import kr.pincoin.api.external.s3.properties.S3Properties
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.time.LocalDateTime
import java.util.*

@Service
class S3HealthCheckService(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    private val logger = KotlinLogging.logger {}
    private val testFileName = "health-check-test.txt"
    private val testContent = "S3 Health Check Test - ${UUID.randomUUID()}"

    /**
     * 간단한 연결 테스트 (빠른 확인용)
     * 기본 속성 검증 + 버킷 접근 테스트
     */
    suspend fun quickHealthCheck(): Unit =
        withContext(Dispatchers.IO) {
            try {
                // 기본 속성 검증 (빠른 체크)
                validateBasicProperties()

                withTimeout(5000) { // 5초 타임아웃
                    val headBucketRequest = HeadBucketRequest.builder()
                        .bucket(s3Properties.bucketName)
                        .build()

                    s3Client.headBucket(headBucketRequest)
                }
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: NoSuchBucketException) {
                throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
            } catch (e: S3Exception) {
                handleS3Exception(e, "빠른 헬스체크")
            } catch (e: BusinessException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "빠른 헬스체크 실패" }
                throw BusinessException(S3ErrorCode.CONNECTION_FAILED)
            }
        }

    /**
     * S3 전체 헬스체크 (설정 검증 + 권한 테스트)
     */
    suspend fun performHealthCheck(): Unit =
        withContext(Dispatchers.IO) {
            try {
                // 1. 설정 검증
                validateConfiguration()

                // 2. 버킷 접근 테스트
                checkBucketAccess()

                // 3. 권한 테스트 (업로드 -> 읽기 -> 삭제)
                testAllPermissions()
            } catch (e: BusinessException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "전체 헬스체크 중 예상치 못한 오류 발생" }
                throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
            }
        }

    /**
     * 기본 속성 검증 (빠른 체크용)
     */
    private fun validateBasicProperties() {
        // 더미값 체크
        if (s3Properties.accessKey == "dummy-access-key") {
            throw BusinessException(S3ErrorCode.ACCESS_DENIED)
        }

        if (s3Properties.secretKey == "dummy-secret-key") {
            throw BusinessException(S3ErrorCode.ACCESS_DENIED)
        }

        if (s3Properties.bucketName == "dummy-bucket") {
            throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
        }

        // 버킷명 형식 체크
        if (!isValidBucketName(s3Properties.bucketName)) {
            throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
        }
    }

    /**
     * S3 설정 검증 (전체 체크용)
     */
    private fun validateConfiguration() {

        // Credentials 검증
        if (s3Properties.accessKey.isBlank() || s3Properties.accessKey == "dummy-access-key") {
            throw BusinessException(S3ErrorCode.ACCESS_DENIED)
        }

        if (s3Properties.secretKey.isBlank() || s3Properties.secretKey == "dummy-secret-key") {
            throw BusinessException(S3ErrorCode.ACCESS_DENIED)
        }

        // 버킷명 검증
        if (s3Properties.bucketName.isBlank() || s3Properties.bucketName == "dummy-bucket") {
            throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
        }

        if (!isValidBucketName(s3Properties.bucketName)) {
            throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
        }

        // 지역 검증
        if (!isValidRegion(s3Properties.region)) {
            throw BusinessException(S3ErrorCode.CONNECTION_FAILED)
        }
    }

    /**
     * 버킷 접근 테스트
     */
    private suspend fun checkBucketAccess() {
        try {
            withTimeout(s3Properties.timeout) {
                val headBucketRequest = HeadBucketRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .build()

                s3Client.headBucket(headBucketRequest)
            }
        } catch (_: TimeoutCancellationException) {
            throw BusinessException(S3ErrorCode.TIMEOUT)
        } catch (_: NoSuchBucketException) {
            throw BusinessException(S3ErrorCode.BUCKET_NOT_FOUND)
        } catch (e: S3Exception) {
            handleS3Exception(e, "버킷 접근")
        } catch (e: Exception) {
            logger.error(e) { "버킷 접근 중 예상치 못한 오류 발생" }
            throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
        }
    }

    /**
     * 모든 권한 테스트 (업로드 -> 읽기 -> 삭제)
     */
    private suspend fun testAllPermissions() {
        val testKey = "health-check/$testFileName"

        try {
            // 1. 업로드 테스트
            withTimeout(s3Properties.timeout) {
                val putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key(testKey)
                    .contentType("text/plain")
                    .metadata(
                        mapOf(
                            "health-check" to "true",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                    .build()

                s3Client.putObject(putObjectRequest, RequestBody.fromString(testContent))
            }

            // 2. 읽기 테스트
            withTimeout(s3Properties.timeout) {
                val getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key(testKey)
                    .build()

                s3Client.getObject(getObjectRequest).use { response ->
                    val content = response.readAllBytes().toString(Charsets.UTF_8)
                    if (content != testContent) {
                        throw BusinessException(S3ErrorCode.FILE_READ_FAILED)
                    }
                }
            }

            // 3. 삭제 테스트
            withTimeout(s3Properties.timeout) {
                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key(testKey)
                    .build()

                s3Client.deleteObject(deleteObjectRequest)
            }

        } catch (_: TimeoutCancellationException) {
            throw BusinessException(S3ErrorCode.TIMEOUT)
        } catch (e: S3Exception) {
            handleS3Exception(e, "권한 테스트")
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "권한 테스트 중 예상치 못한 오류 발생" }
            throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
        }
    }

    /**
     * S3 예외 처리 공통 로직
     */
    private fun handleS3Exception(e: S3Exception, operation: String) {
        val errorMessage = e.awsErrorDetails()?.errorMessage() ?: e.message
        val errorCode = e.awsErrorDetails()?.errorCode()
        val statusCode = e.statusCode()

        logger.error {
            "$operation 실패 - 상태코드: $statusCode, 오류코드: $errorCode, 메시지: $errorMessage, " +
                    "Access Key: ${s3Properties.accessKey.take(8)}..., 지역: ${s3Properties.region}, " +
                    "버킷명: ${s3Properties.bucketName}"
        }

        val businessErrorCode = when (statusCode) {
            400 -> S3ErrorCode.CONNECTION_FAILED
            403 -> S3ErrorCode.ACCESS_DENIED
            404 -> S3ErrorCode.BUCKET_NOT_FOUND
            413 -> S3ErrorCode.FILE_UPLOAD_FAILED
            else -> S3ErrorCode.CONNECTION_FAILED
        }
        throw BusinessException(businessErrorCode)
    }

    private fun isValidBucketName(bucketName: String): Boolean {
        return bucketName.matches(Regex("^[a-z0-9][a-z0-9\\-]*[a-z0-9]$")) &&
                bucketName.length in 3..63 &&
                !bucketName.contains("..") &&
                !bucketName.startsWith("xn--") &&
                !bucketName.endsWith("-s3alias")
    }

    private fun isValidRegion(region: String): Boolean {
        val validRegions = setOf(
            "us-east-1", "us-east-2", "us-west-1", "us-west-2",
            "ap-south-1", "ap-northeast-1", "ap-northeast-2", "ap-northeast-3",
            "ap-southeast-1", "ap-southeast-2", "ap-southeast-3",
            "ca-central-1", "eu-central-1", "eu-west-1", "eu-west-2", "eu-west-3",
            "eu-north-1", "eu-south-1", "sa-east-1", "af-south-1", "me-south-1"
        )
        return validRegions.contains(region)
    }
}