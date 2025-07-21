# MariaDB 컨테이너 실행

## `.env`

```properties
PREFIX=zerocan

MARIADB_ROOT_PASSWORD=secure_root_password_123
MARIADB_PASSWORD=secure_zerocan_password_123
```

## `docker-compose.yml`

```yaml
services:
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
  mariadb-data:
    name: ${PREFIX}-mariadb-data
```

## MariaDB 도커 실행

```shell
# MariaDB 실행
docker compose up -d redis

# MariaDB 실행 결과 확인
docker compose ps

# MariaDB 도커 컨테이너 내 CLI 접속
docker compose exec mariadb mariadb -u root -p
Enter password: 

docker compose exec mariadb mariadb -u zerocan -p zerocan
Enter password: 
```

## MariaDB 설정

- [application-local.yml](/src/main/resources/application-local.yml) 파일에 추가

```yaml
spring:
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://mariadb:3306/zerocan
    username: zerocan
    password: secure_zerocan_password_123
    hikari:
      connectionInitSql: "SET NAMES utf8mb4"
```