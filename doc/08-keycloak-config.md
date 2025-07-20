# User 테이블과 keycloakId(UUID)

nullable keycloakId

- 이미 운영 중인 서비스에 Keycloak 도입
- 기존 사용자들은 keycloakId가 없는 상태
- 점진적 마이그레이션 가능

nullable keycloakId

- 신규 프로젝트에서 시스템 복잡도 감소
- 명확한 비즈니스 규칙
- 예외 처리 코드 불필요