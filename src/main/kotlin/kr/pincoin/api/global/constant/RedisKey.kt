package kr.pincoin.api.global.constant

/**
 * Redis 키 상수 정의
 */
object RedisKey {
    // 세션 관련 키
    const val SESSION_PREFIX = "session:"

    // 세션 메타데이터 필드명
    const val EMAIL = "email"
    const val IP_ADDRESS = "ip_address"
    const val USER_AGENT = "user_agent"
    const val ISSUED_AT = "issued_at"
    const val LAST_ACCESS_AT = "last_access_at"
    const val REFRESH_COUNT = "refresh_count"
    const val REFRESH_TOKEN_HASH = "refresh_token_hash"
}