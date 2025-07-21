# 인증/인가 명세

## 인증

- 로그인
    - 요청 body: email, password, rememberMe(true: 리프레시 토큰 HTTP Only 쿠키 응답 포함)
    - 응답 body: JWT 액세스 토큰
- 리프레시
    - 요청 body: 없음 (HTTP Only 쿠키 전송)
    - 응답 body: JWT 액세스 토큰
- 로그아웃
    - HTTP only 쿠키 삭제
- 회원가입
    - Keycloak 사용자 등록 후 User 및 Role 레코드 추가
    - 백엔드 DB 오류로 User 레코드 추가 실패 시 Keycloak 사용자 삭제 보상 트랜잭션 실행
- 일관성 있는 응답 ApiResponse

### 액세스 토큰 획득 절차

1. 프론트엔드 → 백엔드 → Keycloak (백엔드 경유 방식)

장점:

- Client Secret 보호: 백엔드에서만 Client Secret을 관리하므로 노출 위험이 없음
- 토큰 관리 통제: 백엔드에서 토큰 발급/갱신/무효화를 완전히 제어
- 보안 로깅: 모든 인증 요청을 백엔드에서 로깅 및 모니터링 가능
- 추가 검증: 백엔드에서 비즈니스 로직 검증 추가 가능
- Refresh Token 보호: HttpOnly 쿠키로 XSS 공격 방어

단점:

- 네트워크 홉이 하나 더 추가됨
- 백엔드 서버 부하 증가

2. 프론트엔드 → Keycloak (백엔드 경유 없이 직접 호출)

장점:

- 네트워크 지연 시간 단축
- 백엔드 서버 부하 감소
- OAuth2/OIDC 표준 플로우 직접 사용

단점:

- Client Secret 노출 위험: 프론트엔드에서 Client Secret 사용 불가 (Public Client 필요)
- 토큰 보안 취약: JavaScript에서 토큰 직접 관리시 XSS 공격 위험
- 제어권 부족: 백엔드에서 인증 플로우 제어 불가
- 로깅/모니터링 어려움: 인증 시도를 백엔드에서 추적하기 어려움

최적의 조합:

- Cloudflare (무료 플랜)

    - Bot 차단
    - 기본적인 DDoS 방어
    - SSL/TLS 강제

- 백엔드 Redis Rate Limiting (핵심)

    - 사용자별/IP별 정밀 제어
    - 로그인 성공시 제한 해제
    - 계정 잠금 연동

- 모니터링

    - 실시간 공격 탐지
    - 자동 IP 차단 리스트 관리

### Keycloak 중심 토큰 관리

- Redis 용도
    - 세션 메타데이터 저장 (IP, 디바이스 정보 등)
    - 빠른 세션 조회 (사용자별 활성 세션 목록)
    - 관리 기능 (특정 사용자 모든 세션 로그아웃)

## 인가

- Keycloak 웹 콘솔의 Groups/Role Mapping은 비워두거나 최소한으로만 사용
- 인증(Authentication)은 Keycloak에 위임
- 권한 관리(Authorization)는 백엔드에서 직접 관리
- 백엔드에서 스프링시큐리티 서비스 레벨 보안 `@PreAuthorize("isAuthenticated()")`, `@PreAuthorize("hasRole('ADMIN')")`

# User 테이블과 keycloak 매핑 전략

## KeycloakId (UUID)

- nullable keycloakId

    - 신규 프로젝트에서 시스템 복잡도 감소
    - 명확한 비즈니스 규칙
    - 예외 처리 코드 불필요

- nullable keycloakId

    - 이미 운영 중인 서비스에 Keycloak 도입
    - 기존 사용자들은 keycloakId가 없는 상태
    - 점진적 마이그레이션 가능

## 이메일

- 별도 필드 추가 없음
- 이메일 주소 대소문자 구분 입력 오류에 따른 불일치 가능성