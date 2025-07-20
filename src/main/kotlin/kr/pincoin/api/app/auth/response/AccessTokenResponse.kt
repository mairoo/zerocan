package kr.pincoin.api.app.auth.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AccessTokenResponse(
    @JsonProperty("accessToken")
    val accessToken: String,

    @JsonProperty("tokenType")
    val tokenType: String,

    @JsonProperty("expiresIn")
    val expiresIn: Int
) {
    companion object {
        // https://www.oauth.com/oauth2-servers/access-tokens/access-token-response/
        fun of(accessToken: String, expiresIn: Int) = AccessTokenResponse(
            tokenType = "Bearer",    // OAuth 2.0에서 가장 일반적인 타입
            accessToken = accessToken,
            expiresIn = expiresIn    // 초 단위의 만료 시간
        )
    }
}