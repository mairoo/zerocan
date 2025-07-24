package kr.pincoin.api.external.s3.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kr.pincoin.api.external.s3.api.request.S3BulkUploadRequest
import kr.pincoin.api.external.s3.api.request.S3FileUploadRequest
import kr.pincoin.api.external.s3.api.request.S3PresignedUrlRequest
import kr.pincoin.api.external.s3.api.response.*
import kr.pincoin.api.external.s3.error.S3ErrorCode
import kr.pincoin.api.external.s3.properties.S3Properties
import kr.pincoin.api.global.exception.BusinessException
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
    ): S3FileUploadResponse =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    val request = S3FileUploadRequest(
                        file = file,
                        filePath = filePath,
                        metadata = metadata
                    )

                    when (val result = s3ApiClient.uploadFile(request)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "FILE_TOO_LARGE" -> S3ErrorCode.FILE_UPLOAD_FAILED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.FILE_UPLOAD_FAILED
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
            }
        }

    /**
     * 다중 파일 업로드
     */
    suspend fun bulkUpload(
        files: List<MultipartFile>,
        basePath: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): S3BulkUploadResponse =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout * 3) { // 벌크 업로드는 더 긴 타임아웃
                    val request = S3BulkUploadRequest(
                        files = files,
                        basePath = basePath,
                        metadata = metadata
                    )

                    when (val result = s3ApiClient.bulkUpload(request)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.FILE_UPLOAD_FAILED
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
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
    ): S3PresignedUrlResponse =
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

                    when (val result = s3ApiClient.generatePresignedUploadUrl(request)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.SYSTEM_ERROR
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
            }
        }

    /**
     * Presigned URL 생성 (다운로드용)
     */
    suspend fun generateDownloadUrl(
        fileKey: String,
        expirationSeconds: Long = 3600,
    ): S3PresignedUrlResponse =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.generatePresignedDownloadUrl(fileKey, expirationSeconds)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "FILE_NOT_FOUND" -> S3ErrorCode.FILE_NOT_FOUND
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.SYSTEM_ERROR
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
            }
        }

    /**
     * 파일 다운로드
     */
    suspend fun downloadFile(
        fileKey: String,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.downloadFile(fileKey)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "FILE_NOT_FOUND" -> S3ErrorCode.FILE_NOT_FOUND
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.FILE_READ_FAILED
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.FILE_READ_FAILED)
            }
        }

    /**
     * 파일 정보 조회
     */
    suspend fun getFileInfo(
        fileKey: String,
    ): S3FileInfoResponse =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.getFileInfo(fileKey)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "FILE_NOT_FOUND" -> S3ErrorCode.FILE_NOT_FOUND
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.SYSTEM_ERROR
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
            }
        }

    /**
     * 파일 삭제
     */
    suspend fun deleteFile(
        fileKey: String,
    ): S3DeleteResponse =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.deleteFile(fileKey)) {
                        is S3ApiResponse.Success -> result.data
                        is S3ApiResponse.Error -> {
                            val errorCode = when (result.errorCode) {
                                "FILE_NOT_FOUND" -> S3ErrorCode.FILE_NOT_FOUND
                                "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                else -> S3ErrorCode.FILE_DELETE_FAILED
                            }
                            throw BusinessException(errorCode)
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.FILE_DELETE_FAILED)
            }
        }

    /**
     * 파일 존재 여부 확인
     */
    suspend fun fileExists(
        fileKey: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(s3Properties.timeout) {
                    when (val result = s3ApiClient.getFileInfo(fileKey)) {
                        is S3ApiResponse.Success -> true
                        is S3ApiResponse.Error -> {
                            if (result.errorCode == "FILE_NOT_FOUND") {
                                false
                            } else {
                                val errorCode = when (result.errorCode) {
                                    "ACCESS_DENIED" -> S3ErrorCode.ACCESS_DENIED
                                    "TIMEOUT" -> S3ErrorCode.TIMEOUT
                                    else -> S3ErrorCode.SYSTEM_ERROR
                                }
                                throw BusinessException(errorCode)
                            }
                        }
                    }
                }
            } catch (e: BusinessException) {
                throw e
            } catch (_: TimeoutCancellationException) {
                throw BusinessException(S3ErrorCode.TIMEOUT)
            } catch (_: Exception) {
                throw BusinessException(S3ErrorCode.SYSTEM_ERROR)
            }
        }

    /**
     * 파일 크기 검증
     */
    fun validateFileSize(file: MultipartFile) {
        if (file.size > s3Properties.maxFileSize) {
            throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
        }
    }

    /**
     * 파일 확장자 검증
     */
    fun validateFileExtension(fileName: String?) {
        if (fileName.isNullOrBlank()) {
            throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
        }

        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (!s3Properties.allowedExtensions.contains(extension)) {
            throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
        }
    }

    /**
     * 파일 검증 (크기 + 확장자)
     */
    fun validateFile(file: MultipartFile) {
        validateFileSize(file)
        validateFileExtension(file.originalFilename)
    }

    /**
     * 다중 파일 검증
     */
    fun validateBulkFiles(
        files: List<MultipartFile>,
        maxFileCount: Int = 10,
    ) {
        // 파일 개수 검증
        if (files.isEmpty()) {
            throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
        }

        if (files.size > maxFileCount) {
            throw BusinessException(S3ErrorCode.FILE_UPLOAD_FAILED)
        }

        // 각 파일 검증
        files.forEach { file ->
            validateFile(file)
        }
    }

    /**
     * 파일 URL 추출 (S3 키에서 Public URL 생성)
     */
    fun getPublicUrl(fileKey: String): String {
        return if (s3Properties.endpoint != null) {
            "${s3Properties.endpoint}/${s3Properties.bucketName}/${fileKey}"
        } else {
            "https://${s3Properties.bucketName}.s3.${s3Properties.region}.amazonaws.com/${fileKey}"
        }
    }

    /**
     * 파일 키에서 파일명 추출
     */
    fun extractFileName(fileKey: String): String = fileKey.substringAfterLast('/')

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
}