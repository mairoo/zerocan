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
