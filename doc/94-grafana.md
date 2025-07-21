# 최소 Grafana 모니터링 설정

## 개요

Prometheus = 데이터 수집기 + 창고

- 온도계, 습도계처럼 계속 데이터 수집
- 수집한 데이터를 창고에 체계적으로 저장
- Prometheus 웹 노출을 아예 제거

Grafana = 리포트 생성기 + 대시보드

- 창고에서 데이터를 가져와서
- 예쁜 차트와 그래프로 만들어 보여줌

## 디렉토리 구조

```
/opt/docker/pincoin/monitoring/
├── docker-compose.yml
├── .env
├── prometheus/
│   └── prometheus.yml
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── prometheus.yml
        └── dashboards/
            ├── dashboard.yml
            └── spring-boot-dashboard.json
```

```bash
# 디렉토리 생성
sudo mkdir -p /opt/docker/pincoin/monitoring
sudo chown ubuntu:ubuntu /opt/docker/pincoin/monitoring
cd /opt/docker/pincoin/monitoring
mkdir -p prometheus grafana/provisioning/datasources grafana/provisioning/dashboards logs
sudo chown www-data:www-data /opt/docker/pincoin/monitoring/logs
```

## `.env` 파일

```bash
PREFIX=pincoin
GRAFANA_ADMIN_PASSWORD=your_secure_password_here
```

## `docker-compose.yml`

```yaml
services:
  prometheus:
    container_name: ${PREFIX}-prometheus
    image: prom/prometheus:latest
    restart: unless-stopped
    ports:
      - "9091:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    networks:
      - app-network
    environment:
      - TZ=Asia/Seoul
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

  grafana:
    container_name: ${PREFIX}-grafana
    image: grafana/grafana:latest
    restart: unless-stopped
    ports:
      - "3001:3000"
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    environment:
      - TZ=Asia/Seoul
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD}
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_DEFAULT_LOCALE=ko-KR
      - GF_INSTALL_PLUGINS=grafana-clock-panel
    networks:
      - app-network
    depends_on:
      - prometheus
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "10"

networks:
  app-network:
    name: ${PREFIX}-network
    external: true

volumes:
  prometheus-data:
    name: ${PREFIX}-prometheus-data
  grafana-data:
    name: ${PREFIX}-grafana-data
```

## 실제 ip 확인

```shell
docker network inspect pincoin-network
```

## `prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # 스프링부트 백엔드 메트릭 수집
  - job_name: 'spring-boot-backend'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets:
          - 'pincoin-backend-1:8080'
          - 'pincoin-backend-2:8080'
    scrape_timeout: 5s

  # Prometheus 자체 메트릭
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

## `grafana/provisioning/datasources/prometheus.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
```

## `grafana/provisioning/dashboards/dashboard.yml`

```yaml
apiVersion: 1

providers:
  - name: default
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

## 도커 컨테이너 시작

```bash
docker compose up -d
```

## 호스트 nginx 설정

```
# 기존 설정은 그대로 두고, 맨 위에 추가

# ==========================================
# 모니터링 도구 설정
# ==========================================

# HTTP to HTTPS 리다이렉트 (Grafana)
server {
    listen 80;
    listen [::]:80;
    server_name grafana.pincoin.kr;
    return 301 https://$server_name$request_uri;
}

# HTTPS 서버 (Grafana)
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name grafana.pincoin.kr;

    # SSL 인증서 경로
    ssl_certificate /opt/docker/pincoin/ssl/pincoin.kr.pem;
    ssl_certificate_key /opt/docker/pincoin/ssl/pincoin.kr.key;

    # SSL 보안 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 보안 헤더 (Grafana용으로 조정)
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    # X-Frame-Options는 Grafana 임베딩을 위해 제거

    # 요청 크기 제한
    client_max_body_size 10M;

    # 프록시 설정
    proxy_buffer_size 128k;
    proxy_buffers 4 256k;
    proxy_busy_buffers_size 256k;
    proxy_connect_timeout 10s;
    proxy_send_timeout 30s;
    proxy_read_timeout 30s;

    # 로그 설정
    access_log /opt/docker/pincoin/monitoring/logs/grafana-access.log;
    error_log /opt/docker/pincoin/monitoring/logs/grafana-error.log;

    location / {
        proxy_pass http://localhost:3001;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket 지원 (Grafana 실시간 업데이트)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_cache_bypass $http_upgrade;
    }
}
```

## 접속 정보

- **Grafana**: http://localhost:3001
    - 사용자: `admin`
    - 비밀번호: `.env`에서 설정한 값

- **Prometheus**: http://localhost:9091

## 첫 번째 대시보드 만들기

Grafana 접속 후:

1. **+ → Dashboard → Add new panel**
2. **Query**: `jvm_memory_used_bytes{area="heap"}`
3. **Panel title**: "JVM Heap Memory Usage"
4. **Save**

## 권장 첫 번째 메트릭들

1. **JVM 메모리**: `jvm_memory_used_bytes{area="heap"}`
2. **HTTP 요청 수**: `http_server_requests_seconds_count`
3. **CPU 사용률**: `process_cpu_usage`
4. **애플리케이션 시작 시간**: `application_started_time_seconds`

## 도커 이미지 재생성 및 재시작

```
docker compose down
docker volume rm pincoin-grafana-data
docker compose up -d
```

# 검증

## Prometheus 데이터 수집 확인

```shell
# 백엔드가 메트릭을 제공하는지 확인
curl http://localhost:10011/actuator/prometheus | head -20
curl http://localhost:10012/actuator/prometheus | head -20

# jvm 메트릭이 있는지 확인
curl http://localhost:10011/actuator/prometheus | grep jvm_memory

# Prometheus가 백엔드에 접근할 수 있는지 테스트
docker exec pincoin-prometheus wget -qO- http://pincoin-backend-1:8080/actuator/health
```