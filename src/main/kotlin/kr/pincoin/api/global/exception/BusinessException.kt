package kr.pincoin.api.global.exception

import kr.pincoin.api.global.security.exception.error.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
    message: String? = errorCode.message,
    cause: Throwable? = null
) : RuntimeException(message, cause)