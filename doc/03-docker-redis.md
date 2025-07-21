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

# Redis 컨테이너 실행

## `.env`

```properties
PREFIX=zerocan
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

networks:
  app-network:
    name: ${PREFIX}-network
    driver: bridge

volumes:
  redis-data:
    name: ${PREFIX}-redis-data
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