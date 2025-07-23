package kr.pincoin.api.external.s3.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.s3.api.response.S3ApiResponse
import kr.pincoin.api.external.s3.api.response.S3ConfigDiagnosisResponse
import kr.pincoin.api.external.s3.api.response.S3ConfigIssue
import kr.pincoin.api.external.s3.properties.S3Properties
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.time.LocalDateTime
import java.util.*

data class S3HealthCheckResponse(
    val status: String, // SUCCESS, PARTIAL_FAILURE, FAILURE
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val bucketName: String,
    val region: String,
    val endpoint: String?,
    val checks: List<S3CheckResult>
)

data class S3CheckResult(
    val checkName: String,
    val success: Boolean,
    val message: String,
    val duration: Long? = null,
    val errorCode: String? = null
)

@Service
class S3HealthCheckService(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    private val logger = KotlinLogging.logger {}
    private val testFileName = "health-check-test.txt"
    private val testContent = "S3 Health Check Test - ${UUID.randomUUID()}"

    /**
     * S3 연결 및 기본 권한 테스트
     */
    suspend fun performHealthCheck(): S3ApiResponse<S3HealthCheckResponse> =
        withContext(Dispatchers.IO) {
            val checks = mutableListOf<S3CheckResult>()

            try {
                // 1. 버킷 존재 여부 및 접근 권한 확인
                checks.add(checkBucketAccess())

                // 2. 파일 업로드 권한 확인
                checks.add(checkUploadPermission())

                // 3. 파일 읽기 권한 확인
                checks.add(checkReadPermission())

                // 4. 파일 삭제 권한 확인
                checks.add(checkDeletePermission())

                // 5. 버킷 정책 및 설정 확인
                checks.add(checkBucketConfiguration())

                val successCount = checks.count { it.success }
                val status = when {
                    successCount == checks.size -> "SUCCESS"
                    successCount > 0 -> "PARTIAL_FAILURE"
                    else -> "FAILURE"
                }

                S3ApiResponse.Success(
                    S3HealthCheckResponse(
                        status = status,
                        bucketName = s3Properties.bucketName,
                        region = s3Properties.region,
                        endpoint = s3Properties.endpoint,
                        checks = checks
                    )
                )
            } catch (e: Exception) {
                checks.add(
                    S3CheckResult(
                        checkName = "전체 헬스체크",
                        success = false,
                        message = "헬스체크 중 예상치 못한 오류 발생: ${e.message}",
                        errorCode = "SYSTEM_ERROR"
                    )
                )

                S3ApiResponse.Success(
                    S3HealthCheckResponse(
                        status = "FAILURE",
                        bucketName = s3Properties.bucketName,
                        region = s3Properties.region,
                        endpoint = s3Properties.endpoint,
                        checks = checks
                    )
                )
            }
        }

    /**
     * 버킷 존재 여부 및 접근 권한 확인
     */
    private suspend fun checkBucketAccess(): S3CheckResult {
        return try {
            withTimeout(s3Properties.timeout) {
                val startTime = System.currentTimeMillis()

                logger.info { "버킷 접근 테스트 시작 - 버킷: ${s3Properties.bucketName}, 지역: ${s3Properties.region}" }

                val headBucketRequest = HeadBucketRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .build()

                s3Client.headBucket(headBucketRequest)

                val duration = System.currentTimeMillis() - startTime
                logger.info { "버킷 접근 성공 - 소요 시간: ${duration}ms" }

                S3CheckResult(
                    checkName = "버킷 접근 권한",
                    success = true,
                    message = "버킷에 성공적으로 접근했습니다",
                    duration = duration
                )
            }
        } catch (_: NoSuchBucketException) {
            logger.error { "버킷이 존재하지 않음: ${s3Properties.bucketName}" }
            S3CheckResult(
                checkName = "버킷 접근 권한",
                success = false,
                message = "버킷이 존재하지 않습니다: ${s3Properties.bucketName}",
                errorCode = "BUCKET_NOT_FOUND"
            )
        } catch (e: S3Exception) {
            val errorMessage = e.awsErrorDetails()?.errorMessage() ?: e.message
            val errorCode = e.awsErrorDetails()?.errorCode()
            val statusCode = e.statusCode()

            logger.error {
                "S3 오류 발생 - 상태코드: $statusCode, 오류코드: $errorCode, 메시지: $errorMessage"
            }
            logger.error { "AWS 요청 ID: ${e.requestId()}" }

            val detailedMessage = when {
                errorMessage?.contains("authorization header is malformed") == true -> {
                    "인증 헤더 형식 오류 - Credentials 확인 필요. " +
                            "Access Key: ${s3Properties.accessKey.take(8)}..., " +
                            "지역: ${s3Properties.region}"
                }

                errorMessage?.contains("does not exist") == true -> {
                    "버킷이 존재하지 않거나 다른 지역에 있을 수 있습니다. " +
                            "버킷명: ${s3Properties.bucketName}, 지역: ${s3Properties.region}"
                }

                errorMessage?.contains("Access Denied") == true -> {
                    "버킷 접근 권한이 없습니다. IAM 정책을 확인하세요."
                }

                errorMessage?.contains("InvalidAccessKeyId") == true -> {
                    "Access Key ID가 유효하지 않습니다. AWS 콘솔에서 확인하세요."
                }

                errorMessage?.contains("SignatureDoesNotMatch") == true -> {
                    "Secret Key가 올바르지 않습니다. AWS 콘솔에서 확인하세요."
                }

                else -> "버킷 접근 실패: $errorMessage (상태코드: $statusCode, 오류코드: $errorCode)"
            }

            S3CheckResult(
                checkName = "버킷 접근 권한",
                success = false,
                message = detailedMessage,
                errorCode = when (statusCode) {
                    400 -> "BAD_REQUEST"
                    403 -> "ACCESS_DENIED"
                    404 -> "BUCKET_NOT_FOUND"
                    else -> "S3_ERROR"
                }
            )
        } catch (_: TimeoutCancellationException) {
            logger.error { "버킷 접근 시간 초과" }
            S3CheckResult(
                checkName = "버킷 접근 권한",
                success = false,
                message = "버킷 접근 시간 초과",
                errorCode = "TIMEOUT"
            )
        } catch (e: Exception) {
            logger.error(e) { "예상치 못한 오류 발생" }
            S3CheckResult(
                checkName = "버킷 접근 권한",
                success = false,
                message = "예상치 못한 오류: ${e.message}",
                errorCode = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * 파일 업로드 권한 확인
     */
    private suspend fun checkUploadPermission(): S3CheckResult {
        return try {
            withTimeout(s3Properties.timeout) {
                val startTime = System.currentTimeMillis()

                val putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key("health-check/$testFileName")
                    .contentType("text/plain")
                    .metadata(
                        mapOf(
                            "health-check" to "true",
                            "timestamp" to LocalDateTime.now().toString()
                        )
                    )
                    .build()

                val requestBody = RequestBody.fromString(testContent)
                s3Client.putObject(putObjectRequest, requestBody)

                val duration = System.currentTimeMillis() - startTime

                S3CheckResult(
                    checkName = "파일 업로드 권한",
                    success = true,
                    message = "테스트 파일 업로드 성공",
                    duration = duration
                )
            }
        } catch (e: S3Exception) {
            S3CheckResult(
                checkName = "파일 업로드 권한",
                success = false,
                message = "파일 업로드 실패: ${e.awsErrorDetails()?.errorMessage() ?: e.message}",
                errorCode = when (e.statusCode()) {
                    403 -> "ACCESS_DENIED"
                    413 -> "FILE_TOO_LARGE"
                    else -> "UPLOAD_ERROR"
                }
            )
        } catch (_: TimeoutCancellationException) {
            S3CheckResult(
                checkName = "파일 업로드 권한",
                success = false,
                message = "파일 업로드 시간 초과",
                errorCode = "TIMEOUT"
            )
        } catch (e: Exception) {
            S3CheckResult(
                checkName = "파일 업로드 권한",
                success = false,
                message = "예상치 못한 오류: ${e.message}",
                errorCode = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * 파일 읽기 권한 확인
     */
    private suspend fun checkReadPermission(): S3CheckResult {
        return try {
            withTimeout(s3Properties.timeout) {
                val startTime = System.currentTimeMillis()

                val getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key("health-check/$testFileName")
                    .build()

                s3Client.getObject(getObjectRequest).use { response ->
                    val content = response.readAllBytes().toString(Charsets.UTF_8)
                    val duration = System.currentTimeMillis() - startTime

                    if (content == testContent) {
                        S3CheckResult(
                            checkName = "파일 읽기 권한",
                            success = true,
                            message = "테스트 파일 읽기 성공",
                            duration = duration
                        )
                    } else {
                        S3CheckResult(
                            checkName = "파일 읽기 권한",
                            success = false,
                            message = "파일 내용이 일치하지 않습니다",
                            errorCode = "CONTENT_MISMATCH"
                        )
                    }
                }
            }
        } catch (_: NoSuchKeyException) {
            S3CheckResult(
                checkName = "파일 읽기 권한",
                success = false,
                message = "테스트 파일을 찾을 수 없습니다 (업로드가 실패했을 수 있습니다)",
                errorCode = "FILE_NOT_FOUND"
            )
        } catch (e: S3Exception) {
            S3CheckResult(
                checkName = "파일 읽기 권한",
                success = false,
                message = "파일 읽기 실패: ${e.awsErrorDetails()?.errorMessage() ?: e.message}",
                errorCode = "READ_ERROR"
            )
        } catch (_: TimeoutCancellationException) {
            S3CheckResult(
                checkName = "파일 읽기 권한",
                success = false,
                message = "파일 읽기 시간 초과",
                errorCode = "TIMEOUT"
            )
        } catch (e: Exception) {
            S3CheckResult(
                checkName = "파일 읽기 권한",
                success = false,
                message = "예상치 못한 오류: ${e.message}",
                errorCode = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * 파일 삭제 권한 확인
     */
    private suspend fun checkDeletePermission(): S3CheckResult {
        return try {
            withTimeout(s3Properties.timeout) {
                val startTime = System.currentTimeMillis()

                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .key("health-check/$testFileName")
                    .build()

                s3Client.deleteObject(deleteObjectRequest)

                val duration = System.currentTimeMillis() - startTime

                S3CheckResult(
                    checkName = "파일 삭제 권한",
                    success = true,
                    message = "테스트 파일 삭제 성공",
                    duration = duration
                )
            }
        } catch (e: S3Exception) {
            S3CheckResult(
                checkName = "파일 삭제 권한",
                success = false,
                message = "파일 삭제 실패: ${e.awsErrorDetails()?.errorMessage() ?: e.message}",
                errorCode = "DELETE_ERROR"
            )
        } catch (_: TimeoutCancellationException) {
            S3CheckResult(
                checkName = "파일 삭제 권한",
                success = false,
                message = "파일 삭제 시간 초과",
                errorCode = "TIMEOUT"
            )
        } catch (e: Exception) {
            S3CheckResult(
                checkName = "파일 삭제 권한",
                success = false,
                message = "예상치 못한 오류: ${e.message}",
                errorCode = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * 버킷 설정 확인
     */
    private suspend fun checkBucketConfiguration(): S3CheckResult {
        return try {
            withTimeout(s3Properties.timeout) {
                val startTime = System.currentTimeMillis()

                // 버킷 위치 확인
                val getBucketLocationRequest = GetBucketLocationRequest.builder()
                    .bucket(s3Properties.bucketName)
                    .build()

                val locationResponse = s3Client.getBucketLocation(getBucketLocationRequest)
                val bucketRegion = locationResponse.locationConstraint()?.toString() ?: "us-east-1"

                val duration = System.currentTimeMillis() - startTime

                if (bucketRegion == s3Properties.region ||
                    (bucketRegion == "us-east-1" && s3Properties.region == "us-east-1")
                ) {
                    S3CheckResult(
                        checkName = "버킷 설정",
                        success = true,
                        message = "버킷 지역 설정이 올바릅니다 (${bucketRegion})",
                        duration = duration
                    )
                } else {
                    S3CheckResult(
                        checkName = "버킷 설정",
                        success = false,
                        message = "버킷 지역 불일치: 설정=${s3Properties.region}, 실제=${bucketRegion}",
                        errorCode = "REGION_MISMATCH"
                    )
                }
            }
        } catch (e: S3Exception) {
            S3CheckResult(
                checkName = "버킷 설정",
                success = false,
                message = "버킷 설정 확인 실패: ${e.awsErrorDetails()?.errorMessage() ?: e.message}",
                errorCode = "CONFIG_ERROR"
            )
        } catch (_: TimeoutCancellationException) {
            S3CheckResult(
                checkName = "버킷 설정",
                success = false,
                message = "버킷 설정 확인 시간 초과",
                errorCode = "TIMEOUT"
            )
        } catch (e: Exception) {
            S3CheckResult(
                checkName = "버킷 설정",
                success = false,
                message = "예상치 못한 오류: ${e.message}",
                errorCode = "SYSTEM_ERROR"
            )
        }
    }

    /**
     * 간단한 연결 테스트 (빠른 확인용)
     */
    suspend fun quickHealthCheck(): S3ApiResponse<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(5000) { // 5초 타임아웃
                    val headBucketRequest = HeadBucketRequest.builder()
                        .bucket(s3Properties.bucketName)
                        .build()

                    s3Client.headBucket(headBucketRequest)
                    S3ApiResponse.Success(true)
                }
            } catch (_: Exception) {
                S3ApiResponse.Success(false)  // 에러 상세정보 없이 단순히 false 반환
            }
        }

    /**
     * S3 설정 진단
     */
    suspend fun diagnoseConfiguration(): S3ApiResponse<S3ConfigDiagnosisResponse> =
        withContext(Dispatchers.IO) {
            val issues = mutableListOf<S3ConfigIssue>()
            val recommendations = mutableListOf<String>()

            try {
                // 1. Credentials 형식 검증
                if (s3Properties.accessKey.isBlank() || s3Properties.accessKey == "dummy-access-key") {
                    issues.add(
                        S3ConfigIssue(
                            category = "CREDENTIALS",
                            severity = "CRITICAL",
                            issue = "Access Key가 설정되지 않았거나 기본값입니다",
                            suggestion = "올바른 AWS Access Key를 설정하세요"
                        )
                    )
                }

                if (s3Properties.secretKey.isBlank() || s3Properties.secretKey == "dummy-secret-key") {
                    issues.add(
                        S3ConfigIssue(
                            category = "CREDENTIALS",
                            severity = "CRITICAL",
                            issue = "Secret Key가 설정되지 않았거나 기본값입니다",
                            suggestion = "올바른 AWS Secret Key를 설정하세요"
                        )
                    )
                }

                // 2. 버킷명 검증
                if (s3Properties.bucketName.isBlank() || s3Properties.bucketName == "dummy-bucket") {
                    issues.add(
                        S3ConfigIssue(
                            category = "BUCKET",
                            severity = "CRITICAL",
                            issue = "버킷명이 설정되지 않았거나 기본값입니다",
                            suggestion = "실제 S3 버킷명을 설정하세요"
                        )
                    )
                } else if (!isValidBucketName(s3Properties.bucketName)) {
                    issues.add(
                        S3ConfigIssue(
                            category = "BUCKET",
                            severity = "HIGH",
                            issue = "버킷명 형식이 올바르지 않습니다",
                            suggestion = "S3 버킷 명명 규칙을 따르세요 (소문자, 숫자, 하이픈만 사용)"
                        )
                    )
                }

                // 3. 지역 설정 검증
                if (!isValidRegion(s3Properties.region)) {
                    issues.add(
                        S3ConfigIssue(
                            category = "REGION",
                            severity = "HIGH",
                            issue = "올바르지 않은 AWS 지역입니다",
                            suggestion = "유효한 AWS 지역 코드를 사용하세요 (예: ap-northeast-2)"
                        )
                    )
                }

                // 4. 엔드포인트 설정 검증
                s3Properties.endpoint?.let { endpoint ->
                    if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                        issues.add(
                            S3ConfigIssue(
                                category = "ENDPOINT",
                                severity = "MEDIUM",
                                issue = "엔드포인트 URL 형식이 올바르지 않습니다",
                                suggestion = "http:// 또는 https://로 시작하는 완전한 URL을 사용하세요"
                            )
                        )
                    }
                }

                // 5. 타임아웃 설정 검증
                if (s3Properties.timeout < 1000 || s3Properties.timeout > 300000) {
                    issues.add(
                        S3ConfigIssue(
                            category = "TIMEOUT",
                            severity = "LOW",
                            issue = "타임아웃 설정이 권장 범위를 벗어났습니다",
                            suggestion = "1초(1000ms)에서 5분(300000ms) 사이의 값을 사용하세요"
                        )
                    )
                }

                // 권장사항 생성
                if (issues.any { it.category == "CREDENTIALS" }) {
                    recommendations.add("AWS IAM에서 새로운 Access Key를 생성하고 적절한 S3 권한을 부여하세요")
                }

                if (issues.any { it.category == "BUCKET" }) {
                    recommendations.add("AWS S3 콘솔에서 버킷이 존재하는지 확인하고, 정확한 버킷명을 사용하세요")
                }

                if (s3Properties.endpoint != null) {
                    recommendations.add("커스텀 엔드포인트를 사용 중입니다. LocalStack이나 다른 S3 호환 서비스인지 확인하세요")
                }

                val severity = when {
                    issues.any { it.severity == "CRITICAL" } -> "CRITICAL"
                    issues.any { it.severity == "HIGH" } -> "HIGH"
                    issues.any { it.severity == "MEDIUM" } -> "MEDIUM"
                    issues.any { it.severity == "LOW" } -> "LOW"
                    else -> "NONE"
                }

                S3ApiResponse.Success(
                    S3ConfigDiagnosisResponse(
                        overallStatus = if (issues.isEmpty()) "HEALTHY" else "ISSUES_FOUND",
                        severity = severity,
                        issues = issues,
                        recommendations = recommendations,
                        configSummary = mapOf(
                            "bucketName" to s3Properties.bucketName,
                            "region" to s3Properties.region,
                            "hasCustomEndpoint" to (s3Properties.endpoint != null),
                            "endpoint" to (s3Properties.endpoint ?: "AWS Default"),
                            "timeout" to "${s3Properties.timeout}ms",
                            "accessKeyPrefix" to "${s3Properties.accessKey.take(4)}****"
                        )
                    )
                )
            } catch (e: Exception) {
                S3ApiResponse.Error(
                    errorCode = "DIAGNOSIS_ERROR",
                    errorMessage = "설정 진단 중 오류 발생: ${e.message}"
                )
            }
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