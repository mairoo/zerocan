# 시퀀스 채번 전략

## DBMS 시퀀스 또는 autoincrement 활용

```
-- MySQL 예시
CREATE TABLE daily_sequence (
    seq_date DATE NOT NULL,
    current_val BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (seq_date)
);

-- 시퀀스 값 가져오기
INSERT INTO daily_sequence (seq_date, current_val)
VALUES (CURRENT_DATE, 1)
ON DUPLICATE KEY UPDATE
current_val = LAST_INSERT_ID(current_val + 1);

SELECT LAST_INSERT_ID();
```

## Redis 기반 분산 락

### Redis 키 패턴:

```
firm:bank:seq:{yyyyMMdd}:{disbursementId}
```

### Hash 구조 예시

```
firm:bank:seq:20240113:1001 -> 1
firm:bank:seq:20240113:1002 -> 5
firm:bank:seq:20240113:1003 -> 3

firm:bank:seq:20240114:1001 -> 1
firm:bank:seq:20240114:1002 -> 1
```

각 구성요소 설명:

- `firm:bank:seq`: 고정 접두사 (KEY_PREFIX)
- `yyyyMMdd`: 날짜 (예: 20240113)
- `disbursementId`: 모계좌의 고유 ID (예: 1001, 1002)
- 값: 해당 날짜의 시퀀스 값

예를 들어 `firm:bank:seq:20240113:1001`이 1이라는 값을 가지고 있다면, 이는 disbursementId 1001인 계좌의 2024년 1월 13일자 첫 번째 시퀀스라는 의미입니다.

매일 자정이 되면 모든 키가 만료되어 다음 날 다시 1부터 시작합니다.

### Redis와 DB를 이용한 이중화 전략

- Redis를 주 시퀀스 생성기로 사용하여 성능 최적화
- Redis 장애 시 DB fallback으로 안정성 확보
- Redis 정상 작동 시에도 DB에 백업하여 데이터 정합성 유지

# Redis 명령어

맥북
```
redis-cli
```

도커
```
docker exec -it firmbank-proxy-redis redis-cli
```