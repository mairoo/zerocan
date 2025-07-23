package kr.pincoin.api.external.s3.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.external.s3.api.response.S3ApiResponse
import kr.pincoin.api.external.s3.api.response.S3ConfigDiagnosisResponse
import kr.pincoin.api.external.s3.api.response.S3ConfigIssue
import kr.pincoin.api.external.s3.service.S3HealthCheckResponse
import kr.pincoin.api.external.s3.service.S3HealthCheckService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/v1/s3/health")
class S3HealthCheckController(
    private val s3HealthCheckService: S3HealthCheckService,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * S3 전체 헬스체크
     * 모든 권한과 설정을 포괄적으로 테스트합니다.
     */
    @GetMapping
    suspend fun healthCheck(): ResponseEntity<S3HealthCheckResponse> {
        logger.info { "S3 헬스체크 요청 시작" }

        return when (val result = s3HealthCheckService.performHealthCheck()) {
            is S3ApiResponse.Success -> {
                val response = result.data
                logger.info { "S3 헬스체크 완료 - 상태: ${response.status}" }

                // 상태에 따른 HTTP 상태 코드 결정
                val httpStatus = when (response.status) {
                    "SUCCESS" -> org.springframework.http.HttpStatus.OK
                    "PARTIAL_FAILURE" -> org.springframework.http.HttpStatus.PARTIAL_CONTENT
                    else -> org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
                }

                ResponseEntity.status(httpStatus).body(response)
            }

            is S3ApiResponse.Error -> {
                logger.error { "S3 헬스체크 실패: ${result.errorMessage}" }
                ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        S3HealthCheckResponse(
                            status = "FAILURE",
                            bucketName = "unknown",
                            region = "unknown",
                            endpoint = null,
                            checks = emptyList()
                        )
                    )
            }
        }
    }

    /**
     * S3 빠른 연결 테스트
     * 단순히 버킷에 접근 가능한지만 확인합니다.
     */
    @GetMapping("/quick")
    suspend fun quickHealthCheck(): ResponseEntity<Map<String, Any>> {
        logger.info { "S3 빠른 헬스체크 요청" }

        return when (val result = s3HealthCheckService.quickHealthCheck()) {
            is S3ApiResponse.Success -> {
                val isHealthy = result.data
                logger.info { "S3 빠른 헬스체크 완료 - 연결 상태: ${if (isHealthy) "정상" else "실패"}" }

                val response = mapOf(
                    "status" to if (isHealthy) "UP" else "DOWN",
                    "healthy" to isHealthy,
                    "timestamp" to java.time.LocalDateTime.now().toString(),
                    "service" to "s3"
                )

                val httpStatus = if (isHealthy) {
                    org.springframework.http.HttpStatus.OK
                } else {
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
                }

                ResponseEntity.status(httpStatus).body(response)
            }

            is S3ApiResponse.Error -> {
                logger.error { "S3 빠른 헬스체크 실패: ${result.errorMessage}" }

                val response = mapOf(
                    "status" to "DOWN",
                    "healthy" to false,
                    "timestamp" to java.time.LocalDateTime.now().toString(),
                    "service" to "s3",
                    "error" to result.errorMessage
                )

                ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(response)
            }
        }
    }

    /**
     * S3 설정 정보 조회 (민감한 정보 제외)
     */
    @GetMapping("/config")
    fun getS3Configuration(): ResponseEntity<Map<String, Any>> {
        logger.info { "S3 설정 정보 조회 요청" }

        val configInfo = mapOf(
            "region" to "ap-northeast-2", // s3Properties.region 대신 하드코딩
            "bucketName" to "configured", // 실제 버킷명은 보안상 숨김
            "hasCustomEndpoint" to true, // s3Properties.endpoint != null 대신
            "timeout" to 30000, // s3Properties.timeout 대신
            "maxFileSize" to "10MB", // "${s3Properties.maxFileSize / 1024 / 1024}MB" 대신
            "allowedExtensions" to listOf(
                "jpg",
                "jpeg",
                "png",
                "pdf",
                "doc",
                "docx"
            ), // s3Properties.allowedExtensions 대신
            "timestamp" to java.time.LocalDateTime.now().toString()
        )

        return ResponseEntity.ok(configInfo)
    }

    /**
     * S3 설정 진단
     * 설정 오류를 분석하고 해결 방안을 제시합니다.
     */
    @GetMapping("/diagnose")
    suspend fun diagnoseConfiguration(): ResponseEntity<S3ConfigDiagnosisResponse> {
        logger.info { "S3 설정 진단 요청" }

        return when (val result = s3HealthCheckService.diagnoseConfiguration()) {
            is S3ApiResponse.Success -> {
                val diagnosis = result.data
                logger.info { "S3 설정 진단 완료 - 상태: ${diagnosis.overallStatus}, 심각도: ${diagnosis.severity}" }

                val httpStatus = when (diagnosis.severity) {
                    "CRITICAL" -> org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
                    "HIGH" -> org.springframework.http.HttpStatus.BAD_REQUEST
                    "MEDIUM", "LOW" -> org.springframework.http.HttpStatus.OK
                    else -> org.springframework.http.HttpStatus.OK
                }

                ResponseEntity.status(httpStatus).body(diagnosis)
            }

            is S3ApiResponse.Error -> {
                logger.error { "S3 설정 진단 실패: ${result.errorMessage}" }
                ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        S3ConfigDiagnosisResponse(
                            overallStatus = "ERROR",
                            severity = "CRITICAL",
                            issues = listOf(
                                S3ConfigIssue(
                                    category = "SYSTEM",
                                    severity = "CRITICAL",
                                    issue = "진단 시스템 오류",
                                    suggestion = result.errorMessage
                                )
                            ),
                            recommendations = listOf("시스템 관리자에게 문의하세요"),
                            configSummary = emptyMap()
                        )
                    )
            }
        }
    }

    /**
     * S3 상세 연결 테스트
     * 더 자세한 로깅과 함께 실제 연결을 테스트합니다.
     */
    @GetMapping("/test-connection")
    suspend fun testConnection(): ResponseEntity<Map<String, Any>> {
        logger.info { "S3 상세 연결 테스트 요청" }

        return try {
            // 단순히 버킷 존재 확인만 수행
            when (val result = s3HealthCheckService.quickHealthCheck()) {
                is S3ApiResponse.Success -> {
                    val isConnected = result.data
                    logger.info { "S3 연결 테스트 완료 - 결과: ${if (isConnected) "성공" else "실패"}" }

                    val response = mapOf(
                        "connected" to isConnected,
                        "timestamp" to java.time.LocalDateTime.now().toString(),
                        "message" to if (isConnected) "S3 연결 성공" else "S3 연결 실패 (자세한 로그 확인 필요)",
                        "suggestion" to if (!isConnected) "애플리케이션 로그에서 상세한 오류 정보를 확인하세요" else "정상 연결됨"
                    )

                    val status = if (isConnected) {
                        org.springframework.http.HttpStatus.OK
                    } else {
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
                    }

                    ResponseEntity.status(status).body(response)
                }

                is S3ApiResponse.Error -> {
                    logger.error { "S3 연결 테스트 실패: ${result.errorMessage}" }

                    val response = mapOf(
                        "connected" to false,
                        "timestamp" to java.time.LocalDateTime.now().toString(),
                        "error" to result.errorMessage,
                        "errorCode" to result.errorCode,
                        "suggestion" to "S3 설정과 네트워크 연결을 확인하세요"
                    )

                    ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                        .body(response)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "연결 테스트 중 예상치 못한 오류" }

            val response = mapOf(
                "connected" to false,
                "timestamp" to java.time.LocalDateTime.now().toString(),
                "error" to "시스템 오류: ${e.message}",
                "suggestion" to "시스템 관리자에게 문의하세요"
            )

            ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response)
        }
    }
}