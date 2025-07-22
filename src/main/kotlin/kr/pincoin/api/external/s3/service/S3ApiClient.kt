package kr.pincoin.api.external.s3.service

import kr.pincoin.api.external.s3.api.request.S3BulkUploadRequest
import kr.pincoin.api.external.s3.api.request.S3FileUploadRequest
import kr.pincoin.api.external.s3.api.request.S3PresignedUrlRequest
import kr.pincoin.api.external.s3.api.response.*
import kr.pincoin.api.external.s3.properties.S3Properties
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class S3ApiClient(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties,
) {
    /**
     * 단일 파일 업로드
     */
    suspend fun uploadFile(
        request: S3FileUploadRequest,
    ): S3ApiResponse<S3FileUploadResponse> {
        return try {
            val fileKey = generateFileKey(request.file.originalFilename, request.filePath)
            val fileId = UUID.randomUUID().toString()

            // 파일 확장자 검증
            if (!isAllowedFileExtension(request.file.originalFilename)) {
                return S3ApiResponse.Error("INVALID_FILE_TYPE", "허용되지 않은 파일 형식입니다")
            }

            // 파일 크기 검증
            if (request.file.size > s3Properties.maxFileSize) {
                return S3ApiResponse.Error("FILE_TOO_LARGE", "파일 크기가 제한을 초과했습니다")
            }

            val metadata = mutableMapOf<String, String>().apply {
                put("file-id", fileId)
                put("original-filename", request.file.originalFilename ?: "unknown")
                put("upload-timestamp", LocalDateTime.now().toString())
                putAll(request.metadata)
            }

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .contentType(request.file.contentType)
                .contentLength(request.file.size)
                .metadata(metadata)
                .build()

            val requestBody = RequestBody.fromInputStream(
                request.file.inputStream,
                request.file.size
            )

            val response = s3Client.putObject(putObjectRequest, requestBody)

            S3ApiResponse.Success(
                S3FileUploadResponse(
                    fileId = fileId,
                    fileName = extractFileName(fileKey),
                    originalFileName = request.file.originalFilename ?: "unknown",
                    fileUrl = generateFileUrl(fileKey),
                    fileKey = fileKey,
                    fileSize = request.file.size,
                    contentType = request.file.contentType ?: "application/octet-stream",
                    uploadedAt = LocalDateTime.now(),
                    eTag = response.eTag()?.replace("\"", ""),
                    versionId = response.versionId(),
                    metadata = metadata,
                )
            )
        } catch (e: S3Exception) {
            handleS3Error(e, "파일 업로드")
        } catch (e: Exception) {
            handleGenericError(e, "파일 업로드")
        }
    }

    /**
     * Presigned URL 생성 (업로드용)
     */
    suspend fun generatePresignedUploadUrl(
        request: S3PresignedUrlRequest,
    ): S3ApiResponse<S3PresignedUrlResponse> {
        return try {
            val fileKey = generateFileKey(request.fileName, request.filePath)

            // 파일 확장자 검증
            if (!isAllowedFileExtension(request.fileName)) {
                return S3ApiResponse.Error("INVALID_FILE_TYPE", "허용되지 않은 파일 형식입니다")
            }

            val metadata = mutableMapOf<String, String>().apply {
                put("original-filename", request.fileName)
                put("presigned-timestamp", LocalDateTime.now().toString())
                putAll(request.metadata)
            }

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .contentType(request.contentType)
                .metadata(metadata)
                .build()

            val presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(request.expirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build()

            val presignedRequest = s3Presigner.presignPutObject(presignRequest)
            val expiresAt = LocalDateTime.now().plusSeconds(request.expirationSeconds)

            S3ApiResponse.Success(
                S3PresignedUrlResponse(
                    url = presignedRequest.url().toString(),
                    fileKey = fileKey,
                    expiresIn = request.expirationSeconds,
                    expiresAt = expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        } catch (e: S3Exception) {
            handleS3Error(e, "Presigned URL 생성")
        } catch (e: Exception) {
            handleGenericError(e, "Presigned URL 생성")
        }
    }

    /**
     * Presigned URL 생성 (다운로드용)
     */
    suspend fun generatePresignedDownloadUrl(
        fileKey: String,
        expirationSeconds: Long = 3600,
    ): S3ApiResponse<S3PresignedUrlResponse> {
        return try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .build()

            val presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .getObjectRequest(getObjectRequest)
                .build()

            val presignedRequest = s3Presigner.presignGetObject(presignRequest)
            val expiresAt = LocalDateTime.now().plusSeconds(expirationSeconds)

            S3ApiResponse.Success(
                S3PresignedUrlResponse(
                    url = presignedRequest.url().toString(),
                    fileKey = fileKey,
                    expiresIn = expirationSeconds,
                    expiresAt = expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        } catch (e: S3Exception) {
            handleS3Error(e, "Presigned 다운로드 URL 생성")
        } catch (e: Exception) {
            handleGenericError(e, "Presigned 다운로드 URL 생성")
        }
    }

    /**
     * 파일 다운로드 (바이트 배열 반환)
     */
    suspend fun downloadFile(
        fileKey: String,
    ): S3ApiResponse<ByteArray> {
        return try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .build()

            s3Client.getObject(getObjectRequest).use { response ->
                val bytes = response.readAllBytes()
                S3ApiResponse.Success(bytes)
            }
        } catch (_: NoSuchKeyException) {
            S3ApiResponse.Error("FILE_NOT_FOUND", "파일을 찾을 수 없습니다")
        } catch (e: S3Exception) {
            handleS3Error(e, "파일 다운로드")
        } catch (e: Exception) {
            handleGenericError(e, "파일 다운로드")
        }
    }

    /**
     * 파일 정보 조회
     */
    suspend fun getFileInfo(
        fileKey: String,
    ): S3ApiResponse<S3FileInfoResponse> {
        return try {
            val headObjectRequest = HeadObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .build()

            val response = s3Client.headObject(headObjectRequest)

            val fileId = response.metadata()["file-id"] ?: UUID.randomUUID().toString()
            val originalFileName = response.metadata()["original-filename"] ?: extractFileName(fileKey)

            S3ApiResponse.Success(
                S3FileInfoResponse(
                    fileId = fileId,
                    fileName = extractFileName(fileKey),
                    originalFileName = originalFileName,
                    fileKey = fileKey,
                    fileSize = response.contentLength(),
                    contentType = response.contentType() ?: "application/octet-stream",
                    lastModified = LocalDateTime.ofInstant(response.lastModified(), java.time.ZoneId.systemDefault()),
                    eTag = response.eTag().replace("\"", ""),
                    metadata = response.metadata()
                )
            )
        } catch (_: NoSuchKeyException) {
            S3ApiResponse.Error("FILE_NOT_FOUND", "파일을 찾을 수 없습니다")
        } catch (e: S3Exception) {
            handleS3Error(e, "파일 정보 조회")
        } catch (e: Exception) {
            handleGenericError(e, "파일 정보 조회")
        }
    }

    /**
     * 파일 삭제
     */
    suspend fun deleteFile(
        fileKey: String,
    ): S3ApiResponse<S3DeleteResponse> {
        return try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucketName)
                .key(fileKey)
                .build()

            s3Client.deleteObject(deleteObjectRequest)

            S3ApiResponse.Success(
                S3DeleteResponse(
                    fileId = UUID.randomUUID().toString(), // 실제로는 DB에서 조회해야 함
                    fileKey = fileKey,
                    deleted = true,
                    deletedAt = LocalDateTime.now()
                )
            )
        } catch (e: S3Exception) {
            handleS3Error(e, "파일 삭제")
        } catch (e: Exception) {
            handleGenericError(e, "파일 삭제")
        }
    }

    /**
     * 벌크 업로드
     */
    suspend fun bulkUpload(
        request: S3BulkUploadRequest,
    ): S3ApiResponse<S3BulkUploadResponse> {
        return try {
            val successFiles = mutableListOf<S3FileUploadResponse>()
            val failedFiles = mutableListOf<S3UploadFailure>()

            request.files.forEach { file ->
                val uploadRequest = S3FileUploadRequest(
                    file = file,
                    filePath = request.basePath,
                    metadata = request.metadata
                )

                when (val result = uploadFile(uploadRequest)) {
                    is S3ApiResponse.Success -> successFiles.add(result.data)
                    is S3ApiResponse.Error -> failedFiles.add(
                        S3UploadFailure(
                            fileName = file.originalFilename ?: "unknown",
                            errorCode = result.errorCode,
                            errorMessage = result.errorMessage
                        )
                    )
                }
            }

            S3ApiResponse.Success(
                S3BulkUploadResponse(
                    successFiles = successFiles,
                    failedFiles = failedFiles,
                    totalCount = request.files.size,
                    successCount = successFiles.size,
                    failureCount = failedFiles.size
                )
            )
        } catch (e: Exception) {
            handleGenericError(e, "벌크 업로드")
        }
    }

    /**
     * 파일 키 생성
     */
    private fun generateFileKey(
        originalFileName: String?,
        customPath: String?,
    ): String {
        val fileName = originalFileName ?: "unknown"
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = fileName.substringAfterLast('.', "")

        val generatedFileName = if (extension.isNotEmpty()) {
            "${fileName.substringBeforeLast('.')}_${timestamp}_${uuid}.${extension}"
        } else {
            "${fileName}_${timestamp}_${uuid}"
        }

        return if (customPath.isNullOrBlank()) {
            "uploads/${LocalDateTime.now().year}/${LocalDateTime.now().monthValue}/${generatedFileName}"
        } else {
            "${customPath.trimEnd('/')}/${generatedFileName}"
        }
    }

    /**
     * 파일 URL 생성
     */
    private fun generateFileUrl(
        fileKey: String,
    ): String =
        if (s3Properties.endpoint != null) {
            "${s3Properties.endpoint}/${s3Properties.bucketName}/${fileKey}"
        } else {
            "https://${s3Properties.bucketName}.s3.${s3Properties.region}.amazonaws.com/${fileKey}"
        }

    /**
     * 파일명 추출
     */
    private fun extractFileName(
        fileKey: String,
    ): String =
        fileKey.substringAfterLast('/')

    /**
     * 허용된 파일 확장자 검증
     */
    private fun isAllowedFileExtension(
        fileName: String?,
    ): Boolean {
        if (fileName.isNullOrBlank()) return false
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return s3Properties.allowedExtensions.contains(extension)
    }

    /**
     * S3 에러 처리
     */
    private fun handleS3Error(
        error: S3Exception,
        operation: String,
    ): S3ApiResponse<Nothing> {
        val errorCode = when (error.statusCode()) {
            400 -> "BAD_REQUEST"
            403 -> "ACCESS_DENIED"
            404 -> when (operation) {
                "파일 다운로드", "파일 정보 조회" -> "FILE_NOT_FOUND"
                else -> "BUCKET_NOT_FOUND"
            }
            409 -> "BUCKET_ALREADY_EXISTS"
            413 -> "FILE_TOO_LARGE"
            else -> "S3_ERROR"
        }

        return S3ApiResponse.Error(
            errorCode = errorCode,
            errorMessage = "$operation 중 S3 오류 발생: ${error.awsErrorDetails()?.errorMessage() ?: error.message}"
        )
    }

    /**
     * 일반 에러 처리
     */
    private fun handleGenericError(
        error: Throwable,
        operation: String,
    ): S3ApiResponse<Nothing> =
        S3ApiResponse.Error(
            errorCode = "SYSTEM_ERROR",
            errorMessage = "$operation 중 오류 발생: ${error.message ?: "알 수 없는 오류"}"
        )
}