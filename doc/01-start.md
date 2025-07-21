# 스프링부트/코틀린 프로젝트 생성

## https://start.spring.io/

- 의존성 추가 없이 빈 프로젝트

- Project: Gradle- Kotlin
- Language: Kotlin
- Project Metadata
    - Group: kr.pincoin
    - Artifact: api
    - Name: api
    - Description
    - Package name: kr.pincoin.api
    - Packaging: Jar
    - Java: 24

## ignore 파일 추가

- [.gitignore](/.gitignore)
- [.dockerignore](/.dockerignore)

## git 저장소 연결

```
cd repo
git init
```

원격 저장소 생성 후 연결

# 빌드 및 실행 설정

## [build.gradle.kts](/build.gradle.kts) 수정

- 서드파티 라이브러리 버전 관리를 위한 상수 선언
- QueryDSL 지원

## [application-local.yml](/src/main/resources/application-local.yml) 파일 추가

- 실행 프로파일 변경: local
- `spring.datasource` 알맞게 변경

```yaml
spring:
  application:
    name: api
  jpa:
    hibernate:
      ddl-auto: validate # (spring.jpa.generate-ddl 옵션 미사용)
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        type.descriptor.sql: trace
        jdbc.batch_size: 50
        order_inserts: true
        order_updates: true
    open-in-view: false # 트랜잭션 경계 설정
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://218.145.71.213:3306/database
    username: username
    password: password
    hikari:
      connectionInitSql: "SET NAMES utf8mb4"
```

# 개발환경 설정

```
~/Projects/zerocan/backend/.env
~/Projects/zerocan/backend/docker-compose.yml
~/Projects/zerocan/backend/repo/
~/Projects/zerocan/backend/repo/src/main/resources/application-local.yml
```

```
docker compose up -d redis 
docker compose up -d keycloak-postgres keycloak 
```

keycloak 설정

- realm: zerocan
- client: zerocan-backend

스프링부트 설정: `application-local.yml`

```
>     url: jdbc:mariadb://192.168.0.1:3306/database
>     username: username
>     password: password

>             client-secret: your-client-secret

>   client-secret: your-client-secret
```