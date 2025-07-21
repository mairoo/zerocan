# 도커 설치

- 맥북에서 Docker Desktop 또는 Rancher Desktop 사용

## 디렉토리 구성

```
~/Projects/zerocan/backend/
~/Projects/zerocan/backend/repo
~/Projects/zerocan/backend/repo/Dockerfile.repo
~/Projects/zerocan/backend/.env
~/Projects/zerocan/backend/docker-compose.yml
```

# Redis / MariaDB 컨테이너 실행

## `.env`

```properties
PREFIX=zerocan

MARIADB_ROOT_PASSWORD=secure_root_password_123
MARIADB_PASSWORD=secure_zerocan_password_123
```

## `docker-compose.yml`

```yaml
services:
  redis:
    container_name: ${PREFIX}-redis
    image: redis:alpine
    restart: unless-stopped
    ports:
      - "16379:6379"
    volumes:
      - redis-data:/data
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

  mariadb:
    container_name: ${PREFIX}-mariadb
    image: mariadb:lts
    restart: unless-stopped
    ports:
      - "13306:3306"
    volumes:
      - mariadb-data:/var/lib/mysql
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
      - MYSQL_ROOT_PASSWORD=${MARIADB_ROOT_PASSWORD}
      - MYSQL_DATABASE=zerocan
      - MYSQL_USER=zerocan
      - MYSQL_PASSWORD=${MARIADB_PASSWORD}
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

networks:
  app-network:
    name: ${PREFIX}-network
    driver: bridge

volumes:
  redis-data:
    name: ${PREFIX}-redis-data
  mariadb-data:
    name: ${PREFIX}-mariadb-data
```

## redis 도커 실행

```shell
# Redis 실행
docker compose up -d redis

# Redis 실행 결과 확인
docker compose ps

# Redis 도커 컨테이너 내 CLI 접속
docker compose exec redis redis-cli
```

## Redis 설정

- [RedisConfig](/src/main/kotlin/kr/pincoin/api/global/config/RedisConfig.kt) 추가
- [application-local.yml](/src/main/resources/application-local.yml) 파일에 추가

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20  # 기본 페이지 사이즈
        max-page-size: 200 # 최대 페이지 사이즈값을 기본값과 동일하게
    redis:
      host: zerocan-redis  # 도커 redis 컨테이너 이름
      port: 6379 # 컨테이너 내부 포트 접근 가능
      repositories: # RedisTemplate 사용, Redis Repository 미사용
        enabled: false
```

## MariaDB 도커 실행

```shell
docker compose exec mariadb mariadb -u root -p
Enter password: 

docker compose exec mariadb mariadb -u zerocan -p zerocan
Enter password: 
```

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