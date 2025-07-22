package kr.pincoin.api.external.s3.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.s3.api.request.S3BulkUploadRequest
import kr.pincoin.api.external.s3.api.request.S3FileUploadRequest
import kr.pincoin.api.external.s3.api.request.S3PresignedUrlRequest
import kr.pincoin.api.external.s3.api.response.*
import kr.pincoin.api.external.s3.properties.S3Properties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class S3FileService(
    private val s3ApiClient: S3ApiClient,
    private val s3Properties: S3Properties,
) {
    /**
     * 파일 업로드
     */
    suspend fun uploadFile(
        file: MultipartFile,
        filePath: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): S3ApiResponse<S3FileUploadResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    val request = S3FileUploadRequest(
                        file = file,
                        filePath = filePath,
                        metadata = metadata
                    )

                    s3ApiClient.uploadFile(request)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("파일 업로드")
            } catch (e: Exception) {
                handleError(e, "파일 업로드")
            }
        }

    /**
     * 다중 파일 업로드
     */
    suspend fun bulkUpload(
        files: List<MultipartFile>,
        basePath: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): S3ApiResponse<S3BulkUploadResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout * 3) { // 벌크 업로드는 더 긴 타임아웃
                    val request = S3BulkUploadRequest(
                        files = files,
                        basePath = basePath,
                        metadata = metadata
                    )

                    s3ApiClient.bulkUpload(request)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("다중 파일 업로드")
            } catch (e: Exception) {
                handleError(e, "다중 파일 업로드")
            }
        }

    /**
     * Presigned URL 생성 (업로드용)
     */
    suspend fun generateUploadUrl(
        fileName: String,
        contentType: String,
        filePath: String? = null,
        expirationSeconds: Long = 3600,
        metadata: Map<String, String> = emptyMap(),
    ): S3ApiResponse<S3PresignedUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    val request = S3PresignedUrlRequest(
                        fileName = fileName,
                        contentType = contentType,
                        filePath = filePath,
                        expirationSeconds = expirationSeconds,
                        metadata = metadata
                    )

                    s3ApiClient.generatePresignedUploadUrl(request)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("업로드 URL 생성")
            } catch (e: Exception) {
                handleError(e, "업로드 URL 생성")
            }
        }

    /**
     * Presigned URL 생성 (다운로드용)
     */
    suspend fun generateDownloadUrl(
        fileKey: String,
        expirationSeconds: Long = 3600,
    ): S3ApiResponse<S3PresignedUrlResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    s3ApiClient.generatePresignedDownloadUrl(fileKey, expirationSeconds)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("다운로드 URL 생성")
            } catch (e: Exception) {
                handleError(e, "다운로드 URL 생성")
            }
        }

    /**
     * 파일 다운로드
     */
    suspend fun downloadFile(
        fileKey: String,
    ): S3ApiResponse<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    s3ApiClient.downloadFile(fileKey)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("파일 다운로드")
            } catch (e: Exception) {
                handleError(e, "파일 다운로드")
            }
        }

    /**
     * 파일 정보 조회
     */
    suspend fun getFileInfo(
        fileKey: String,
    ): S3ApiResponse<S3FileInfoResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    s3ApiClient.getFileInfo(fileKey)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("파일 정보 조회")
            } catch (e: Exception) {
                handleError(e, "파일 정보 조회")
            }
        }

    /**
     * 파일 삭제
     */
    suspend fun deleteFile(
        fileKey: String,
    ): S3ApiResponse<S3DeleteResponse> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    s3ApiClient.deleteFile(fileKey)
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("파일 삭제")
            } catch (e: Exception) {
                handleError(e, "파일 삭제")
            }
        }

    /**
     * 파일 존재 여부 확인
     */
    suspend fun fileExists(
        fileKey: String,
    ): S3ApiResponse<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.getFileInfo(fileKey)) {
                        is S3ApiResponse.Success -> S3ApiResponse.Success(true)
                        is S3ApiResponse.Error -> {
                            if (result.errorCode == "FILE_NOT_FOUND") {
                                S3ApiResponse.Success(false)
                            } else {
                                S3ApiResponse.Error(result.errorCode, result.errorMessage)
                            }
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                handleTimeout("파일 존재 확인")
            } catch (e: Exception) {
                handleError(e, "파일 존재 확인")
            }
        }

    /**
     * 파일 크기 검증
     */
    fun validateFileSize(
        file: MultipartFile,
    ): S3ApiResponse<Unit> {
        return if (file.size > s3Properties.maxFileSize) {
            S3ApiResponse.Error(
                "FILE_TOO_LARGE",
                "파일 크기가 제한을 초과했습니다. 최대 크기: ${s3Properties.maxFileSize / 1024 / 1024}MB"
            )
        } else {
            S3ApiResponse.Success(Unit)
        }
    }

    /**
     * 파일 확장자 검증
     */
    fun validateFileExtension(
        fileName: String?,
    ): S3ApiResponse<Unit> {
        if (fileName.isNullOrBlank()) {
            return S3ApiResponse.Error("INVALID_FILENAME", "파일명이 올바르지 않습니다")
        }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        return if (s3Properties.allowedExtensions.contains(extension)) {
            S3ApiResponse.Success(Unit)
        } else {
            S3ApiResponse.Error(
                "INVALID_FILE_TYPE",
                "허용되지 않은 파일 형식입니다. 허용된 확장자: ${s3Properties.allowedExtensions.joinToString(", ")}"
            )
        }
    }

    /**
     * 파일 검증 (크기 + 확장자)
     */
    fun validateFile(
        file: MultipartFile,
    ): S3ApiResponse<Unit> {
        // 크기 검증
        validateFileSize(file).let { sizeResult ->
            if (sizeResult is S3ApiResponse.Error) return sizeResult
        }

        // 확장자 검증
        validateFileExtension(file.originalFilename).let { extensionResult ->
            if (extensionResult is S3ApiResponse.Error) return extensionResult
        }

        return S3ApiResponse.Success(Unit)
    }

    /**
     * 다중 파일 검증
     */
    fun validateBulkFiles(
        files: List<MultipartFile>,
        maxFileCount: Int = 10,
    ): S3ApiResponse<Unit> {
        // 파일 개수 검증
        if (files.isEmpty()) {
            return S3ApiResponse.Error("NO_FILES", "업로드할 파일이 없습니다")
        }

        if (files.size > maxFileCount) {
            return S3ApiResponse.Error(
                "TOO_MANY_FILES",
                "파일 개수가 제한을 초과했습니다. 최대 ${maxFileCount}개까지 업로드 가능합니다"
            )
        }

        // 각 파일 검증
        files.forEachIndexed { index, file ->
            when (val result = validateFile(file)) {
                is S3ApiResponse.Error -> {
                    return S3ApiResponse.Error(
                        result.errorCode,
                        "파일 ${index + 1} (${file.originalFilename}): ${result.errorMessage}"
                    )
                }

                is S3ApiResponse.Success -> { /* 성공 시 계속 진행 */
                }
            }
        }

        return S3ApiResponse.Success(Unit)
    }

    /**
     * 파일 URL 추출 (S3 키에서 Public URL 생성)
     */
    fun getPublicUrl(
        fileKey: String,
    ): String {
        return if (s3Properties.endpoint != null) {
            "${s3Properties.endpoint}/${s3Properties.bucketName}/${fileKey}"
        } else {
            "https://${s3Properties.bucketName}.s3.${s3Properties.region}.amazonaws.com/${fileKey}"
        }
    }

    /**
     * 파일 키에서 파일명 추출
     */
    fun extractFileName(
        fileKey: String,
    ): String = fileKey.substringAfterLast('/')

    /**
     * 지원하는 파일 확장자 목록 조회
     */
    fun getSupportedExtensions(): List<String> = s3Properties.allowedExtensions

    /**
     * 최대 파일 크기 조회 (바이트)
     */
    fun getMaxFileSize(): Long = s3Properties.maxFileSize

    /**
     * 최대 파일 크기 조회 (MB)
     */
    fun getMaxFileSizeMB(): Long = s3Properties.maxFileSize / 1024 / 1024

    private fun handleTimeout(
        operation: String,
    ): S3ApiResponse<Nothing> =
        S3ApiResponse.Error(
            errorCode = "TIMEOUT",
            errorMessage = "$operation 요청 시간 초과"
        )

    private fun handleError(
        error: Throwable,
        operation: String,
    ): S3ApiResponse<Nothing> =
        S3ApiResponse.Error(
            errorCode = "SYSTEM_ERROR",
            errorMessage = "$operation 중 오류 발생: ${error.message ?: "알 수 없는 오류"}"
        )
}