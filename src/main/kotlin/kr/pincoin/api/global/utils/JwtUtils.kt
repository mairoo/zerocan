package kr.pincoin.api.global.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtUtils(
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    /**
     * JWT 토큰에서 jti(JWT ID) 클레임을 추출합니다.
     *
     * @param token JWT 토큰 (Bearer 프리픽스 없이)
     * @return jti 값, 추출 실패시 예외 발생
     * @throws IllegalArgumentException JWT 형식이 잘못되었거나 jti가 없는 경우
     */
    fun extractJti(token: String): String {
        try {
            val payload = extractPayload(token)
            val jti = payload.get("jti")?.asText()

            return jti ?: throw IllegalArgumentException("JWT에 jti 클레임이 없습니다")

        } catch (e: Exception) {
            logger.error { "JWT jti 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            throw IllegalArgumentException("JWT jti 추출 실패: ${e.message}", e)
        }
    }

    /**
     * JWT 토큰에서 subject(sub) 클레임을 추출합니다.
     *
     * @param token JWT 토큰
     * @return subject 값, 추출 실패시 예외 발생
     */
    fun extractSubject(token: String): String {
        try {
            val payload = extractPayload(token)
            val subject = payload.get("sub")?.asText()

            return subject ?: throw IllegalArgumentException("JWT에 sub 클레임이 없습니다")

        } catch (e: Exception) {
            logger.error { "JWT subject 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            throw IllegalArgumentException("JWT subject 추출 실패: ${e.message}", e)
        }
    }

    /**
     * JWT 토큰에서 사용자 이메일을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 이메일 주소, 추출 실패시 예외 발생
     */
    fun extractEmail(token: String): String {
        try {
            val payload = extractPayload(token)

            // 이메일은 여러 클레임에 있을 수 있음 (email, preferred_username 등)
            val email = payload.get("email")?.asText()
                ?: payload.get("preferred_username")?.asText()
                ?: payload.get("username")?.asText()

            return email ?: throw IllegalArgumentException("JWT에서 이메일을 찾을 수 없습니다")

        } catch (e: Exception) {
            logger.error { "JWT 이메일 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            throw IllegalArgumentException("JWT 이메일 추출 실패: ${e.message}", e)
        }
    }

    /**
     * JWT 토큰에서 만료 시간(exp)을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 만료 시간 (Unix timestamp)
     */
    fun extractExpiration(token: String): Long {
        try {
            val payload = extractPayload(token)
            val exp = payload.get("exp")?.asLong()

            return exp ?: throw IllegalArgumentException("JWT에 exp 클레임이 없습니다")

        } catch (e: Exception) {
            logger.error { "JWT 만료시간 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            throw IllegalArgumentException("JWT 만료시간 추출 실패: ${e.message}", e)
        }
    }

    /**
     * JWT 토큰에서 발급 시간(iat)을 추출합니다.
     *
     * @param token JWT 토큰
     * @return 발급 시간 (Unix timestamp)
     */
    fun extractIssuedAt(token: String): Long {
        try {
            val payload = extractPayload(token)
            val iat = payload.get("iat")?.asLong()

            return iat ?: throw IllegalArgumentException("JWT에 iat 클레임이 없습니다")

        } catch (e: Exception) {
            logger.error { "JWT 발급시간 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            throw IllegalArgumentException("JWT 발급시간 추출 실패: ${e.message}", e)
        }
    }

    /**
     * JWT 토큰이 만료되었는지 확인합니다.
     *
     * @param token JWT 토큰
     * @return 만료 여부
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            val expiration = extractExpiration(token)
            val currentTime = System.currentTimeMillis() / 1000
            expiration < currentTime
        } catch (e: Exception) {
            logger.warn { "JWT 만료 확인 실패: token=${token.take(20)}..., error=${e.message}" }
            true // 확인 실패시 만료된 것으로 간주
        }
    }

    /**
     * JWT 토큰에서 특정 클레임을 추출합니다.
     *
     * @param token JWT 토큰
     * @param claimName 클레임 이름
     * @return 클레임 값 (String), 없으면 null
     */
    fun extractClaim(token: String, claimName: String): String? {
        return try {
            val payload = extractPayload(token)
            payload.get(claimName)?.asText()
        } catch (e: Exception) {
            logger.warn { "JWT 클레임 '$claimName' 추출 실패: token=${token.take(20)}..., error=${e.message}" }
            null
        }
    }

    /**
     * JWT 토큰의 페이로드를 추출하고 파싱합니다.
     *
     * @param token JWT 토큰
     * @return 파싱된 페이로드 JsonNode
     * @throws IllegalArgumentException JWT 형식이 잘못된 경우
     */
    private fun extractPayload(token: String): JsonNode {
        // JWT는 header.payload.signature 형태
        val parts = token.split(".")

        if (parts.size != 3) {
            throw IllegalArgumentException("잘못된 JWT 형식입니다. 3개의 part가 필요합니다.")
        }

        try {
            // Base64URL 디코딩
            val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)

            // JSON 파싱
            return objectMapper.readTree(payloadJson)

        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("JWT 페이로드 Base64 디코딩 실패", e)
        } catch (e: Exception) {
            throw IllegalArgumentException("JWT 페이로드 JSON 파싱 실패", e)
        }
    }

    /**
     * JWT 토큰 형식이 유효한지 기본적인 검증을 수행합니다.
     * 서명 검증은 하지 않고 형식만 확인합니다.
     *
     * @param token JWT 토큰
     * @return 형식이 유효한지 여부
     */
    fun isValidFormat(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            // 각 part가 Base64URL로 디코딩 가능한지 확인
            parts.forEach { part ->
                Base64.getUrlDecoder().decode(part)
            }

            // 페이로드가 JSON인지 확인
            val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            objectMapper.readTree(payloadJson)

            true
        } catch (e: Exception) {
            logger.debug { "JWT 형식 검증 실패: token=${token.take(20)}..., error=${e.message}" }
            false
        }
    }

    /**
     * JWT 토큰의 타입을 확인합니다 (access_token, refresh_token 등)
     *
     * @param token JWT 토큰
     * @return 토큰 타입 (typ 클레임 값)
     */
    fun getTokenType(token: String): String? {
        return try {
            val payload = extractPayload(token)
            payload.get("typ")?.asText()
        } catch (e: Exception) {
            logger.debug { "JWT 토큰 타입 확인 실패: token=${token.take(20)}..., error=${e.message}" }
            null
        }
    }

    /**
     * 두 JWT 토큰이 같은 세션인지 확인합니다 (같은 jti인지)
     *
     * @param token1 첫 번째 토큰
     * @param token2 두 번째 토큰
     * @return 같은 세션인지 여부
     */
    fun isSameSession(token1: String, token2: String): Boolean {
        return try {
            val jti1 = extractJti(token1)
            val jti2 = extractJti(token2)
            jti1 == jti2
        } catch (e: Exception) {
            logger.debug { "JWT 세션 비교 실패: error=${e.message}" }
            false
        }
    }
}