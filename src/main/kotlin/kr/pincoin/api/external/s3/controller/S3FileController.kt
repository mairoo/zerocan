package kr.pincoin.api.external.s3.controller

import kr.pincoin.api.external.s3.api.response.S3ApiResponse
import kr.pincoin.api.external.s3.service.S3FileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/v1/files")
class S3FileController(
    private val s3FileService: S3FileService,
) {
    /**
     * 파일 정보 조회
     * GET /open/v1/files/info?key=media/blog/2018-12-06/file.jpg
     */
    @GetMapping("/info")
    suspend fun getFileInfo(
        @RequestParam("key") fileKey: String,
    ): ResponseEntity<*> {
        return when (val result = s3FileService.getFileInfo(fileKey)) {
            is S3ApiResponse.Success -> ResponseEntity.ok(result.data)
            is S3ApiResponse.Error -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Unit)
        }
    }
}