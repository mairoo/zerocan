package kr.pincoin.api.external.s3.error

import kr.pincoin.api.global.error.ErrorCode
import org.springframework.http.HttpStatus

enum class S3ErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : ErrorCode {
    // 연결 관련 오류
    CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "S3 연결에 실패했습니다"),
    TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "S3 요청 시간이 초과되었습니다"),

    // 권한 관련 오류
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "S3 접근 권한이 없습니다"),
    BUCKET_NOT_FOUND(HttpStatus.NOT_FOUND, "S3 버킷을 찾을 수 없습니다"),

    // 파일 관련 오류
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"),
    FILE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 읽기에 실패했습니다"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다"),
    INVALID_FILE_KEY(HttpStatus.BAD_REQUEST, "올바르지 않은 파일 키입니다"),

    // 헬스체크 관련 오류
    HEALTH_CHECK_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "S3 헬스체크에 실패했습니다"),

    // 시스템 오류
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S3 시스템 오류가 발생했습니다"),
}