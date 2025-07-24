package kr.pincoin.api.external.s3.controller

import kr.pincoin.api.external.s3.service.S3HealthCheckService
import kr.pincoin.api.global.response.success.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/s3/healthcheck")
class S3HealthCheckController(
    private val s3HealthCheckService: S3HealthCheckService,
) {
    /**
     * S3 빠른 연결 테스트
     * 기본적인 버킷 접근 가능 여부만 확인
     */
    @GetMapping()
    suspend fun quickHealthCheck(): ResponseEntity<ApiResponse<Map<String, Any>>> =
        s3HealthCheckService.quickHealthCheck()
            .let {
                mapOf(
                    "status" to "UP",
                    "healthy" to true,
                    "timestamp" to java.time.LocalDateTime.now().toString(),
                    "service" to "s3"
                ) as Map<String, Any>
            }
            .let { ApiResponse.of(it, message = "S3 연결이 정상입니다") }
            .let { ResponseEntity.ok(it) }

    /**
     * S3 전체 헬스체크
     * 모든 권한(업로드/다운로드/삭제)과 설정을 포괄적으로 테스트
     * 문제 발생시 상세한 진단 정보 제공
     */
    @GetMapping("/full")
    suspend fun fullHealthCheck(): ResponseEntity<ApiResponse<Map<String, Any>>> =
        s3HealthCheckService.performHealthCheck()
            .let {
                mapOf(
                    "status" to "UP",
                    "healthy" to true,
                    "timestamp" to java.time.LocalDateTime.now().toString(),
                    "service" to "s3",
                    "checks" to listOf(
                        "bucket_access",
                        "upload_permission",
                        "read_permission",
                        "delete_permission",
                        "configuration"
                    )
                )
            }
            .let { ApiResponse.of(it, message = "S3 전체 헬스체크가 성공적으로 완료되었습니다") }
            .let { ResponseEntity.ok(it) }
}