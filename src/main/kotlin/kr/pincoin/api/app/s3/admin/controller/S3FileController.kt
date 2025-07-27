package kr.pincoin.api.app.s3.admin.controller

import kr.pincoin.api.external.s3.api.response.S3FileInfoResponse
import kr.pincoin.api.external.s3.service.S3FileService
import kr.pincoin.api.global.response.success.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/s3/files")
class S3FileController(
    private val s3FileService: S3FileService,
) {
    /**
     * 파일 정보 조회
     * GET /open/s3/files/info?key=media/blog/2018-12-06/file.jpg
     */
    @GetMapping("/info")
    suspend fun getFileInfo(
        @RequestParam("key") fileKey: String,
    ): ResponseEntity<ApiResponse<S3FileInfoResponse>> =
        s3FileService.getFileInfo(fileKey)
            .let { ApiResponse.of(it) }
            .let { ResponseEntity.ok(it) }
}