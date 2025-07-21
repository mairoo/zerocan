package kr.pincoin.api.app.auth.vo

import kr.pincoin.api.app.auth.response.AccessTokenResponse

data class TokenPair(
    val accessToken: AccessTokenResponse,

    val refreshToken: String?,

    val rememberMe: Boolean = false,

    val refreshExpiresIn: Long? = null,
)