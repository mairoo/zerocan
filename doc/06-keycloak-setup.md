# Keycloak 용도

- Keycloak 웹 콘솔의 Groups/Role Mapping은 비워두거나 최소한으로만 사용
- Keycloak을 순수 인증(Authentication)용으로만 사용하고, 권한(Authorization)은 백엔드에서 직접 관리

# 설치

## `.env`

다음 내용 추가

```properties
KEYCLOAK_DB_PASSWORD=secure_db_password_123
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=secure_admin_password_123
```

## `docker-compose.yml`

```yaml
services:
  keycloak-postgres:
    container_name: ${PREFIX}-keycloak-postgres
    image: postgres:15-alpine
    restart: unless-stopped
    volumes:
      - keycloak-postgres-data:/var/lib/postgresql/data
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
      - POSTGRES_DB=keycloak
      - POSTGRES_USER=keycloak
      - POSTGRES_PASSWORD=${KEYCLOAK_DB_PASSWORD:-keycloak123}
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

  keycloak:
    container_name: ${PREFIX}-keycloak
    image: quay.io/keycloak/keycloak:23.0.3
    restart: unless-stopped
    ports:
      - "8082:8080"
    depends_on:
      - keycloak-postgres
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
      - KEYCLOAK_ADMIN=${KEYCLOAK_ADMIN:-admin}
      - KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin123}
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://keycloak-postgres:5432/keycloak
      - KC_DB_USERNAME=keycloak
      - KC_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD:-keycloak123}
      # - KC_HOSTNAME=keycloak
      # - KC_HOSTNAME_PORT=8080
      - KC_HOSTNAME_STRICT=false
      - KC_HOSTNAME_STRICT_HTTPS=false
      - KC_HTTP_ENABLED=true
      - KC_HEALTH_ENABLED=true
      - KC_METRICS_ENABLED=true
    command: start
    volumes:
      - keycloak-data:/opt/keycloak/data
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

volumes:
  keycloak-postgres-data:
    name: ${PREFIX}-keycloak-postgres-data
  keycloak-data:
    name: ${PREFIX}-keycloak-data
```

backend 도커 설정에 추가 사항: 의존성 추가, KEYCLOAK 접속 주소 설정

```yaml
    depends_on:
      - redis
      - keycloak # (1) 추가
    environment:
      - TZ=Asia/Seoul
      - SPRING_PROFILES_ACTIVE=local
      - KEYCLOAK_AUTH_SERVER_URL=http://localhost:8082 # (2) 추가
```

## 도커 실행

```shell
# 도커 실행
docker compose up -d

# 도커 실행 프로세스 확인
docker compose ps

# 도커 로그 확인
docker compose logs -f keycloak

# psql 클라이언트로 직접 접근
docker compose exec keycloak-postgres psql -U keycloak -d keycloak

# postgres 유저로 접근
docker compose exec keycloak-postgres psql -U postgres
```

# 웹 콘솔 설정

## 접속

- http://localhost:8082
- 아이디: KEYCLOAK_ADMIN 설정 값 (예, admin)
- 비밀번호: KEYCLOAK_ADMIN_PASSWORD 설정 값 (예, secure_admin_password_123)

## realm 생성

- Realm 생성: Realms → Create Realm → (Realm name: `realm`)

## client 생성

- Client 생성: Clients → Create Client →
    1. General Settings:
        - **Client type: OpenID Connect**
        - **Client ID: zerocan-backend**
        - Name: (없음)
        - Description: (없음)
        - Always display in UI: OFF
    2. Capability Config:
        - **Client authentication: ON**  (중요!)
        - Authorization: OFF
        - **Standard flow: ON**
        - **Direct access grants: ON**
        - Implicit flow: OFF
        - **Service accounts roles: ON**
        - OAuth 2.0 Device Authorization Grant: OFF
        - OIDC CIBA Grant: OFF
    3. Login Settings
        - Root URL: (없음)
        - Home URL: (없음)
        - Valid redirect URIs: http://localhost:8080/*
        - Valid post logout redirect URIs: (없음)
        - Web origins: (없음)

- `zerocan-backend` 클라이언트 상세 보기 `Service accounts roles` 탭 선택
    - Assign role 버튼 누르고 `Filter by realm roles` 드롭다운에서 `Filter by clients` 선택하여 다음 추가 할당
        - `realm-management`: `manage-users`
        - `realm-management`: `view-users`
        - `realm-management`: `query-users`

- `pincoin-backend` 클라이언트 설정 완료 후 `Credentials` 탭에서 Client Secret 복사

# 스프링부트 설정

## `application.yml`

- http://keycloak:8080: 도커 내부 HTTP 격리된 네트워크
    - 개발: 외부 노출 10013 포트로 웹 콘솔 도커 접근
    - 운영: Cloudflare WAF → 호스트 Nginx (HTTPS) → 도커 내부

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Keycloak JWT 검증 설정 (기존 JWT와 병행 사용)
          issuer-uri: http://keycloak:8080/realms/zerocan
      client:
        registration:
          keycloak:
            client-id: zerocan-backend
            client-secret: your-secret
            scope: openid,profile,email
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          keycloak:
            issuer-uri: http://keycloak:8080/realms/zerocan
            user-name-attribute: preferred_username

keycloak:
  enabled: true # 점진적 마이그레이션을 위한 활성화 플래그
  user-migration: # 기존 사용자 매핑 전략
    auto-create: true # 자동 생성 여부
  # Realm 및 클라이언트 설정
  realm: zerocan
  client-id: zerocan-backend
  client-secret: your-secret
  server-url: http://keycloak:8080
```

## external.auth.keycloak 주요 파일 추가

- [KeycloakProperties](/src/main/kotlin/kr/pincoin/api/external/properties/KeycloakProperties.kt)
- [KeycloakWebClientConfig](/src/main/kotlin/kr/pincoin/api/global/config/KeycloakWebClientConfig.kt)