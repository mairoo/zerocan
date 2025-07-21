# 인프라 옵션 비교

## 1. Vultr.com Cloud Compute / High Performance

| vCPUs    | 메모리  | 대역폭  | 스토리지  | 가격   | 서비스 가능 (예상)      | 동접자 수 (예상) |
|----------|------|------|-------|------|------------------|------------|
| 4 vCPUs  | 12GB | 7TB  | 260GB | $72  | 1개 서비스 (~4.3GB)  | 200~500    |
| 8 vCPUs  | 16GB | 8TB  | 350GB | $96  | 2개 서비스 (~8.6GB)  | 500~1000   |
| 12 vCPUs | 24GB | 12TB | 500GB | $144 | 5개 서비스 (~21.5GB) | 1250~1750  |

- AMD EPYC 4vCPUs (다수 도커 컨테이너 실행 시 멀티 스레드 성능이 Intel Xeon 대비 약간 우세)
- 방화벽 무료 제공
- Auto Backup (월 $14.4) 미사용: github이 소스 코드 백업 장치 역할
- DDOS Protection (월 $10) 미사용: cloudflare 웹 방화벽이 80/443 포트 보호 / 방화벽이 22 포트 보호
- AWS RDS, S3 별도 이용

## 2. 코로케이션 (HP DL360 GEN9)

| CPU        | 메모리  | 스토리지      | 대역폭                             | 서비스 가능 (예상)    | 동접자 수 (예상) |
|------------|------|-----------|---------------------------------|----------------|------------|
| 12코어/24스레드 | 32GB | 500GB SSD | 1000Mbps Dedicated / Max 30Mbps | 7개 서비스 (~28GB) | 1750~2250  |

- Intel Xeon E5-2620V3 2.4GHz × 2EA
- DDR4 32GB ECC 메모리

## 서비스 구성 (공통)

**서비스 1개 예상 메모리 사용량 = ~3.3GB**

- Redis: ~200MB
- Backend-1: ~1GB
- Backend-2: ~1GB
- Backend Nginx: ~50MB
- Frontend-1: ~500MB
- Frontend-2: ~500MB
- Frontend Nginx: ~50MB

**운영체제 + 도커 오버헤드 = ~1GB or ~2GB**

## 순간 최대 속도 비교

- **Vultr High Performance**: 0.5~3Gbps 가능, 순간 1Gbps 초과도 허용됨, 월간 트래픽 제한 있음
- **AWS EC2**: 0.5~400Gbps 가능, 트래픽 무제한 요금 종량제
- **KoreaIDC**: 1Gbps Dedicated/ Max 0.03Gbps (Dedicated와 Max는 모순적 표기)

## 요약

- **KoreaIDC** / 최대 7개 서비스 / 30만원
    - 7개 서비스를 모두 활용하지 못하면 비용 효율성 저하
    - 운영체제 + 도커 오버헤드 고려하면 실질적으로 6개 서비스가 최대
    - 전용선임에도 순간 최대 속도는 클라우드 대비 제한적 (30Mbps)
    - 물리적 장애 시 상당한 다운타임 발생 가능
    - **단일 장애점(SPOF): 하나의 물리 서버에 모든 서비스가 집중되어 전체 서비스 동시 중단 위험**

- **Vultr** / 6개 서비스 / 40만원 (8 vCPUs x 3개 기준)
    - 필요에 따라 유연한 스케일링 가능
    - 순간 대역폭은 우수하나 월간 트래픽 제한
    - 클라우드 관리 편의성
    - **위험 분산: 여러 인스턴스에 서비스 분산으로 부분 장애 시에도 일부 서비스는 정상 운영 가능**

**30만원짜리 사놓고 실제로 5개 쓰면 6만원, 40만원짜리 사놓고 실제로 6개 쓰면 6.67만원**

- docker compose 수준 배포 관리 = 개발자 감당 수준
- docker swarm 또는 kubernetes 도입 수준 배포 관리 = 인프라 관리자 채용 필요

# 우분투 서버

## 기본 설정

```
# 패키지 업데이트
apt-get update && apt-get dist-upgrade
apt-get autormeove
apt-get autoclean

# 타임존 설정
timedatectl set-timezone Asia/Seoul

# 로케일 설정
apt-get install -y language-pack-ko
update-locale LANG=en_US.UTF-8

# 호스트 이름 (필요 시)
hostnamectl
hostnamectl set-hostname my-server-name

# vim 디폴트
update-alternatives --config editor
```

## `ubuntu` 관리 계정 (`ubuntu` 디폴트 계정은 이미 `sudo` 그룹에 속해 있음)

```
visudo
```

```
%sudo   ALL=(ALL:ALL) NOPASSWD: ALL
```

로컬 컴퓨터에서

```
# SSH 키 생성 (이미 있다면 생략)
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

원격 서버에서

```
# ubuntu 계정으로 전환
sudo su - ubuntu

# .ssh 디렉토리 생성
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# authorized_keys 파일 생성
nano ~/.ssh/authorized_keys
# 여기에 로컬의 ~/.ssh/id_rsa.pub 내용 복사 붙여넣기

# 파일 권한 설정
chmod 600 ~/.ssh/authorized_keys
```

루트 원격 접속 금지 및 키 접속만 허용

```
sudo vi /etc/ssh/sshd_config
```

```
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
```

```
sudo service ssh restart
```

## 도커 시스템

https://docs.docker.com/engine/install/ubuntu/#install-using-the-repository

```
# 도커 공식 GPG 키 추가
sudo apt-get update
sudo apt-get install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# APT 소스에 저장소 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update

# 도커 패키지 설치
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 도커 설치 확인
docker run hello-world

sudo mkdir -p /opt/docker
sudo chown ubuntu:ubuntu /opt/docker

# ubuntu 계정 docker 그룹에 추가하여 sudo 권한 필요 없이 접근
sudo usermod -aG docker ubuntu

# 현재 세션에 그룹 변경사항 적용
newgrp docker
```

`.dockerignore`

```dockerignore
**/build/
**/.gradle/
.gradle/
*.log
logs/
```

## 호스트 nginx

### 설치

```
sudo apt-get install nginx
sudo ufw allow "Nginx Full"
sudo ufw status
```

### 설정

`/etc/nginx/sites-enabled/default`

```
server {
listen 80;
listen [::]:80;

    server_name _;
    root /var/www/html;
    index index.html index.htm;
    
    # 서버 정보 숨기기
    server_tokens off;
    
    # 숨김 파일 차단
    location ~ /\. {
        deny all;
    }
    
    # 기본 위치
    location / {
        try_files $uri $uri/ =404;
    }
    
    # PHP 차단 (불필요시)
    location ~ \.php$ {
        return 404;
    }
}
```

`/etc/nginx/nginx.conf`

```
http {
    # 서버 토큰 숨기기
    server_tokens off;
    
    # 파일 업로드 크기 제한
    client_max_body_size 10M;
    
    # 기존 설정들...
}
```

```
# 설정 확인
sudo nginx -t

# 적용
sudo systemctl reload nginx
```