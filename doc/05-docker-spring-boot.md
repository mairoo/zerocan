# 스프링 백엔드 도커 빌드

## [Dockerfile.local](/Dockerfile.local) 추가

## `docker-compose.yml` 파일에 추가

```yaml
services:
  backend:
    container_name: ${PREFIX}-backend
    image: ${PREFIX}-backend:local
    build:
      context: ./repo
      dockerfile: Dockerfile.local
    working_dir: /app
    volumes:
      - ./repo:/app:cached  # 소스코드를 볼륨 마운트
      - gradle-cache:/root/.gradle
    ports:
      - "8080:8080"
    depends_on:
      - redis
      - mariadb
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
      - SPRING_PROFILES_ACTIVE=local
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:8080/actuator/health" ]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  gradle-cache:
    name: ${PREFIX}-gradle-cache
```

# 백엔드 도커 이미지 빌드 및 컨테이너 실행

```shell
# 도커 이미지 빌드
docker compose build backend 

# 도커 컨테이너 실행
docker compose up -d backend 

# 도커 컨테이너 프로세스 목록
docker compose ps

# 백엔드 로그 확인
docker compose logs -f backend
```

# 도커 컨테이너 로깅

- [application-local.yml](/src/main/resources/application-local.yml) 파일에 추가

```yaml
logging:
  level:
    root: INFO # 운영 WARN
    org.hibernate.SQL: DEBUG  # SQL 로그 레벨 / 운영 WARN
    org.hibernate.orm.jdbc.bind: TRACE  # 바인딩 파라미터 로그 레벨 / 운영 WARN
    kr.pincoin.api: DEBUG # 운영 INFO
    org.springframework.web: DEBUG
    org.springframework.jdbc.core.JdbcTemplate: DEBUG
    org.springframework.jdbc.core.StatementCreatorUtils: TRACE
  file:
    name: /app/logs/application.log
```

```shell
# 도커 백엔드 이미지 강제 빌드
docker compose build --no-cache backend

# 도커 백엔드 재시작
docker compose restart backend
```