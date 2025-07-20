# 요건

- 로그인
    - JWT 액세스 토큰
    - rememberMe: true - 리프레시 토큰 HTTP only 쿠키 전송
- 리프레시
    - HTTP only 쿠키 전송이므로 body 없음
    - Redis TTL 토큰
- 로그아웃
    - HTTP only 쿠키 삭제
- 사용자 추가
    - Keycloak 등록 후 User 레코드 추가
    - Role 추가
- 일관성 있는 응답 ApiResponse

# User 테이블과 keycloakId(UUID)

nullable keycloakId

- 이미 운영 중인 서비스에 Keycloak 도입
- 기존 사용자들은 keycloakId가 없는 상태
- 점진적 마이그레이션 가능

nullable keycloakId

- 신규 프로젝트에서 시스템 복잡도 감소
- 명확한 비즈니스 규칙
- 예외 처리 코드 불필요