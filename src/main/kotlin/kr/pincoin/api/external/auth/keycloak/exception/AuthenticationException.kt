package kr.pincoin.api.external.auth.keycloak.exception

import kr.pincoin.api.global.error.ErrorCode

class AuthenticationException(
    private val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message)