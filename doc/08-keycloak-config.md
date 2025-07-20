# 인증/인가 명세

## 인증

- 로그인
    - 요청 body: email, password, rememberMe(true: 리프레시 토큰 HTTP Only 쿠키 응답 포함)
    - 응답 body: JWT 액세스 토큰
- 리프레시
    - 요청 body: 없음 (HTTP Only 쿠키 전송)
    - 응답 body: JWT 액세스 토큰
    - Redis TTL 토큰 저장 (RedisTemplate)
- 로그아웃
    - HTTP only 쿠키 삭제
- 회원가입
    - Keycloak 사용자 등록 후 User 및 Role 레코드 추가
    - 백엔드 DB 오류로 User 레코드 추가 실패 시 Keycloak 사용자 삭제 보상 트랜잭션 실행
- 일관성 있는 응답 ApiResponse
- 로그인, 리프레시, 로그아웃 모두 Keycloak API 의존성

### 리프레시 전략

1. 단순한 토큰만 저장: 메모리 효율적
2. 단일 세션 강제: 보안 중시
3. 멀티 세션 관리: 사용자 경험 중시

```kotlin
/**
 * 전략 1: 단순한 토큰만 저장 (메모리 효율적)
 * - 중복 로그인 허용하는 서비스에 적합
 * - Redis 메모리 사용량 최소화
 */
private fun storeTokensSimple(
    tokenResponse: KeycloakTokenResponse,
    email: String,
    request: HttpServletRequest,
) {
    val refreshToken = tokenResponse.refreshToken ?: return
    val clientIp = IpUtils.getClientIp(request)

    // 리프레시 토큰 정보만 저장
    redisTemplate.opsForHash<String, String>().putAll(
        refreshToken,
        mapOf(
            RedisKey.EMAIL to email,
            RedisKey.IP_ADDRESS to clientIp,
            "issued_at" to System.currentTimeMillis().toString()
        )
    )

    redisTemplate.expire(refreshToken, tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)
}

/**
 * 전략 2: 단일 세션 강제 (보안 중시)
 * - 중복 로그인 방지 필요한 서비스
 * - 금융, 의료 등 보안이 중요한 도메인
 */
private fun storeTokensSingleSession(
    tokenResponse: KeycloakTokenResponse,
    email: String,
    request: HttpServletRequest,
) {
    val refreshToken = tokenResponse.refreshToken ?: return
    val clientIp = IpUtils.getClientIp(request)

    with(redisTemplate) {
        // 기존 토큰 무효화
        opsForValue().get(email)?.let { oldToken ->
            delete(oldToken)
        }

        // 새 토큰 저장
        opsForHash<String, String>().putAll(
            refreshToken,
            mapOf(
                RedisKey.EMAIL to email,
                RedisKey.IP_ADDRESS to clientIp,
                "issued_at" to System.currentTimeMillis().toString()
            )
        )

        expire(refreshToken, tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)

        // 이메일 → 토큰 매핑
        opsForValue().set(
            email,
            refreshToken,
            tokenResponse.refreshExpiresIn,
            TimeUnit.SECONDS
        )
    }
}

/**
 * 전략 3: 멀티 세션 관리 (사용자 경험 중시)
 * - 여러 기기 동시 로그인 허용하면서도 관리 기능 제공
 * - 일반적인 웹/모바일 서비스에 적합
 */
private fun storeTokensMultiSession(
    tokenResponse: KeycloakTokenResponse,
    email: String,
    request: HttpServletRequest,
) {
    val refreshToken = tokenResponse.refreshToken ?: return
    val clientIp = IpUtils.getClientIp(request)
    val userAgent = request.getHeader("User-Agent")

    with(redisTemplate) {
        // 토큰별 상세 정보 저장
        opsForHash<String, String>().putAll(
            refreshToken,
            mapOf(
                RedisKey.EMAIL to email,
                RedisKey.IP_ADDRESS to clientIp,
                "user_agent" to (userAgent ?: ""),
                "issued_at" to System.currentTimeMillis().toString()
            )
        )

        expire(refreshToken, tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)

        // 사용자별 토큰 목록 관리 (Set 사용)
        opsForSet().add("user_sessions:$email", refreshToken)
        expire("user_sessions:$email", tokenResponse.refreshExpiresIn, TimeUnit.SECONDS)
    }
}

/**
 * 멀티 세션에서 사용자의 모든 토큰 무효화
 */
fun invalidateAllUserSessions(email: String) {
    with(redisTemplate) {
        // 사용자의 모든 토큰 조회
        val tokens = opsForSet().members("user_sessions:$email") ?: emptySet()

        // 각 토큰 삭제
        tokens.forEach { token ->
            delete(token)
        }

        // 사용자 세션 목록 삭제
        delete("user_sessions:$email")
    }
}
```

## 인가

- Keycloak 웹 콘솔의 Groups/Role Mapping은 비워두거나 최소한으로만 사용
- 인증(Authentication)은 Keycloak에 위임
- 권한 관리(Authorization)는 백엔드에서 직접 관리
- 백엔드에서 스프링시큐리티 서비스 레벨 보안 `@PreAuthorize("isAuthenticated()")`, `@PreAuthorize("hasRole('ADMIN')")`

# User 테이블과 keycloakId(UUID)

- nullable keycloakId

    - 신규 프로젝트에서 시스템 복잡도 감소
    - 명확한 비즈니스 규칙
    - 예외 처리 코드 불필요

- nullable keycloakId

    - 이미 운영 중인 서비스에 Keycloak 도입
    - 기존 사용자들은 keycloakId가 없는 상태
    - 점진적 마이그레이션 가능
