# 파티션 관리자(PartitionManager) 기술 문서

## 개요

`PartitionManager` 클래스는 데이터베이스 테이블의 파티션을 자동으로 관리하기 위한 컴포넌트입니다. 이 관리자는 다양한 테이블에 대해 일간 또는 월간 단위로 파티션을 생성하고, 보관 기간이 지난 파티션을
삭제하는 기능을 제공합니다.

## 주요 특징

- 테이블별로 커스터마이즈된 파티션 관리 전략 적용
- 일간 및 월간 파티션 지원
- 테이블별 데이터 보관 기간 설정
- 미래 파티션 사전 생성 기능
- 만료된 파티션 자동 제거 기능
- `p_future` 파티션을 통한 예상치 못한 날짜 데이터 처리
- 애플리케이션 시작 시 파티션 초기화 기능

## 관리 대상 테이블

| 테이블명                     | 파티션 타입 | 보관 기간 | 파티션 이름 형식  |
|--------------------------|--------|-------|------------|
| auth_login_log           | 일간     | 3개월   | p_20250228 |
| disbursement_error       | 월간     | 6개월   | p_202502   |
| disbursement_log         | 월간     | 6개월   | p_202502   |
| accounting_journal_entry | 월간     | 24개월  | p_202502   |

## 파티셔닝을 위한 테이블 구조 설정

`PartitionManager`를 사용하기 전에, 각 테이블은 파티셔닝을 지원하도록 적절히 구조화되어야 합니다. 다음은 각 테이블에 필요한 선행 설정 작업입니다.

### 파티셔닝 전 테이블 구조 변경 예시 (auth_login_log)

```sql
-- 1. AUTO_INCREMENT 속성 제거
ALTER TABLE auth_login_log
    MODIFY id bigint(20) NOT NULL;

-- 2. 기본 키 삭제 후 복합 키
ALTER TABLE auth_login_log
    DROP PRIMARY KEY;
ALTER TABLE auth_login_log
    ADD PRIMARY KEY (id, created);

-- 3. AUTO_INCREMENT 속성 추가
ALTER TABLE auth_login_log
    MODIFY id bigint(20) NOT NULL AUTO_INCREMENT;
```

### 중요 고려사항

- MySQL/MariaDB에서 파티셔닝된 테이블은 **파티셔닝 키(여기서는 created)가 반드시 모든 기본 키의 일부**여야 합니다.
- 이러한 테이블 구조 변경은 데이터베이스 관리자가 수동으로 한 번만 설정해야 하는 작업입니다.
- 구조 변경 후, `PartitionManager`가 파티션 생성 및 삭제를 자동으로 관리합니다.
- 기존 데이터가 있는 테이블에 이러한 변경을 적용할 때는 주의가 필요하며, 가능하면 유지보수 시간에 수행해야 합니다.

## 주요 컴포넌트

### 열거형 클래스

#### TableConfig

테이블별 파티션 관리 설정을 정의합니다.

```kotlin
enum class TableConfig(
    val tableName: String,
    val partitionType: PartitionType,
    val retentionMonths: Int
) {
    LOGIN_LOG("auth_login_log", PartitionType.DAILY, 1), // 90일
    DISBURSEMENT_ERROR("disbursement_error", PartitionType.MONTHLY, 6), // 6개월
    DISBURSEMENT_LOG("disbursement_log", PartitionType.MONTHLY, 6), // 6개월
    ACCOUNTING_JOURNAL("accounting_journal_entry", PartitionType.MONTHLY, 24) // 24개월
}
```

#### PartitionType

파티션 타입을 정의합니다.

```kotlin
enum class PartitionType {
    DAILY,
    MONTHLY
}
```

### 주요 메서드

#### 파티션 관리 스케줄러 - managePartitions()

매일 정해진 시간(오전 1시)에 자동 실행되어 모든 테이블의 파티션을 관리합니다.

```kotlin
@Scheduled(cron = "0 0 1 * * *")
fun managePartitions() {
    // 모든 테이블에 대해 파티션 관리 작업 수행
    // 1. 미래 파티션 추가
    // 2. 오래된 파티션 제거
}
```

#### 애플리케이션 시작 시 초기화 - initializePartitions()

애플리케이션 시작 시 테이블별 파티션을 초기화합니다.

```kotlin
@PostConstruct
fun initializePartitions() {
    // 모든 테이블에 대해 파티션 초기화 확인
}
```

#### 미래 파티션 생성 - createFuturePartitions()

지정된 테이블에 미래 파티션을 생성합니다.

```kotlin
private fun createFuturePartitions(config: TableConfig) {
    // 파티션 타입에 따라 미래 날짜 생성 및 파티션 추가
}
```

#### 만료된 파티션 제거 - deleteExpiredPartitions()

보관 기간이 지난 파티션을 식별하여 제거합니다.

```kotlin
private fun deleteExpiredPartitions(config: TableConfig) {
    // 오래된 파티션 식별 및 제거
}
```

## SQL 쿼리

### 파티션 추가 쿼리

#### 일간 파티션 추가

```sql
ALTER TABLE {tableName} ADD PARTITION
    (PARTITION {partitionName} VALUES LESS THAN (TO_DAYS('{nextDay}')))
```

**예시**:

- `tableName`: auth_login_log
- `partitionName`: p_20250301 (2025년 3월 1일 파티션)
- `nextDay`: 2025-03-02 (다음 날짜)

**실제 생성되는 쿼리**:

```sql
ALTER TABLE auth_login_log
    ADD PARTITION
        (PARTITION p_20250301 VALUES LESS THAN (TO_DAYS('2025-03-02')))
```

이 쿼리는 2025년 3월 1일 데이터를 저장하는 파티션을 생성합니다. 이 파티션은 `created` 컬럼의 값이 '2025-03-01 00:00:00'부터 '2025-03-01 23:59:59'까지인 레코드를
저장합니다.

#### 월간 파티션 추가

```sql
ALTER TABLE {tableName} ADD PARTITION
    (PARTITION {partitionName} VALUES LESS THAN (TO_DAYS('{nextMonthDate}')))
```

**예시**:

- `tableName`: disbursement_log
- `partitionName`: p_202503 (2025년 3월 파티션)
- `nextMonthDate`: 2025-04-01 (다음 달 첫 날)

**실제 생성되는 쿼리**:

```sql
ALTER TABLE disbursement_log
    ADD PARTITION
        (PARTITION p_202503 VALUES LESS THAN (TO_DAYS('2025-04-01')))
```

이 쿼리는 2025년 3월 데이터를 저장하는 파티션을 생성합니다. 이 파티션은 `created` 컬럼의 값이 '2025-03-01 00:00:00'부터 '2025-03-31 23:59:59'까지인 레코드를
저장합니다.

#### MAXVALUE 파티션 추가

```sql
ALTER TABLE {tableName} ADD PARTITION (PARTITION p_future VALUES LESS THAN MAXVALUE)
```

**예시**:

- `tableName`: auth_login_log

**실제 생성되는 쿼리**:

```sql
ALTER TABLE auth_login_log
    ADD PARTITION (PARTITION p_future VALUES LESS THAN MAXVALUE)
```

### p_future 파티션의 목적과 역할

`p_future` 파티션은 정의되지 않은 미래 날짜의 데이터를 임시로 저장하기 위한 특수 파티션입니다.

#### p_future 파티션의 특징

1. `p_future` 파티션은 항상 파티션 범위의 마지막에 위치하며, 모든 파티션에 해당하지 않는 데이터를 저장합니다.
2. `VALUES LESS THAN MAXVALUE`로 정의되어 명시적인 파티션이 없는 모든 날짜의 데이터를 포함합니다.
3. 파티션 관리 과정에서 새 파티션을 추가할 때 일시적으로 제거했다가 다시 추가하는 방식으로 관리됩니다.

#### 데이터 관리 측면에서의 역할

1. **예외 데이터 처리**: `PartitionManager`의 설계상:

- 일별 파티션의 경우 현재 날짜 + 7일까지의 파티션을 미리 생성합니다.
- 월별 파티션의 경우 현재 월 + 3개월까지의 파티션을 미리 생성합니다.
- 이 범위를 벗어나는 미래 날짜 데이터는 모두 `p_future`에 저장됩니다.

2. **데이터 안전성**: 정상적인 운영 상황에서는:

- 일반적인 데이터는 이미 생성된 구체적인 날짜/월 파티션에 저장됩니다.
- 파티션 스케줄러가 매일 실행되어 미래 파티션을 계속 생성하므로, 정상적인 데이터가 `p_future`에 들어갈 가능성은 매우 낮습니다.
- 이상적으로는 `p_future` 파티션은 항상 비어있거나 아주 소량의 데이터만 포함해야 합니다.

3. **문제 감지**: `p_future` 파티션에 데이터가 지속적으로 쌓인다면:

- 이는 파티션 생성 로직이나 데이터 입력 과정에 문제가 있을 수 있습니다.
- 별도의 모니터링을 통해 이런 상황을 감지하는 것이 좋습니다.

#### 파티션 관리 중 p_future의 동작

`PartitionManager`에서 새 파티션을 추가할 때:

1. `p_future` 파티션을 일시적으로 제거합니다.
2. 새로운 범위 파티션을 추가합니다.
3. `p_future` 파티션을 다시 추가합니다.

이 과정에서 특정 날짜의 파티션(예: `p_20250325`)에 데이터를 삽입하는 작업에는 영향이 없습니다:

- 각 파티션은 독립적으로 작동하며, MySQL/MariaDB는 파티션 키(날짜)를 확인하여 데이터가 어떤 파티션에 들어갈지 결정합니다.
- `p_future`가 일시적으로 제거되는 동안에도 이미 존재하는 파티션으로의 데이터 삽입은 정상적으로 진행됩니다.
- 단, 이 짧은 시간 동안에는 아직 파티션이 생성되지 않은 날짜의 데이터 삽입이 실패할 수 있습니다.

그러나 코드 로직에 따르면, 이 전체 과정은 매우 짧은 시간 내에 완료되므로 실제로 데이터 삽입에 문제가 생길 가능성은 극히 낮습니다.

### 파티션 삭제 쿼리

```sql
ALTER TABLE {tableName} DROP PARTITION {partitionName}
```

### 파티션 조회 쿼리

```sql
SELECT PARTITION_NAME
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = ?
    [
  AND PARTITION_NAME != 'p_future'] -- 옵션: p_future 제외
ORDER BY PARTITION_NAME
```

### 파티션 존재 확인 쿼리

```sql
SELECT COUNT(*)
FROM information_schema.PARTITIONS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = ?
  AND [PARTITION_NAME = ? | PARTITION_NAME IS NOT NULL]
```

### 초기 파티션 생성 쿼리

```sql
ALTER TABLE {tableName}
    PARTITION BY RANGE (TO_DAYS(created)) (
    PARTITION p_yyyyMMdd VALUES LESS THAN (TO_DAYS('next-date')),
    ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
    );
```

## 파티션 관리 시나리오 예시

### 1. auth_login_log (일간 파티션, 보관기간 3개월)

* 파티션 이름 형식: p_20250228 형식(일별)
* 매일 실행 시:
    * 현재 날짜 기준으로 향후 7일치 일간 파티션 생성
    * 3개월(90일) 이전 파티션 삭제
* 유지되는 파티션 수: 약 97개(보관 90일 + 현재 + 미래 7일)
* 예시:
    * 2025-02-28 실행: p_20250301~p_20250307 생성
    * 2025-03-01 실행: p_20250308 생성
    * 2025-05-30 실행: p_20250228과 이전 파티션 삭제

### 2. disbursement_error (월간 파티션, 보관기간 6개월)

* 파티션 이름 형식: p_202502 형식(월별)
* 매일 실행 시:
    * 현재 월 기준으로 향후 3개월치 월간 파티션 생성
    * 6개월 이전 파티션 삭제
* 유지되는 파티션 수: 약 9개(보관 6개월 + 현재 + 미래 3개월)
* 예시:
    * 2025-02-28 실행: p_202503~p_202505 생성
    * 2025-03-01 실행: 이미 파티션 존재하므로 추가 없음
    * 2025-04-01 실행: p_202506 생성
    * 2025-09-01 실행: p_202502와 이전 파티션 삭제

### 3. disbursement_log (월간 파티션, 보관기간 6개월)

* 파티션 이름 형식: p_202502 형식(월별)
* 관리 방식은 disbursement_error와 동일

### 4. accounting_journal_entry (월간 파티션, 보관기간 24개월)

* 파티션 이름 형식: p_202502 형식(월별)
* 매일 실행 시:
    * 현재 월 기준으로 향후 3개월치 월간 파티션 생성
    * 24개월 이전 파티션 삭제
* 유지되는 파티션 수: 약 27개(보관 24개월 + 현재 + 미래 3개월)
* 예시:
    * 2025-02-28 실행: p_202503~p_202505 생성
    * 2025-03-01 실행: 이미 파티션 존재하므로 추가 없음
    * 2025-04-01 실행: p_202506 생성
    * 2027-03-01 실행: p_202502와 이전 파티션 삭제

## 오류 처리 방식

1. 파티션 관리 중 오류가 발생하면 로그에 기록
2. p_future 파티션이 제거된 상태에서 오류 발생 시 복원 시도
3. 관리자에게 알림 메시지 전송 (`sendAdminNotification` 메서드)

## 아카이빙 기능

* `archivePartitionData()` 메서드를 통해 파티션 삭제 전 아카이빙 수행 가능
* 현재 구현은 로깅만 하고 있으므로, 필요시 실제 데이터 아카이빙 로직 추가 필요

## 공통 참고사항

* 모든 테이블은 p_future 파티션을 항상 유지하여 정의되지 않은 날짜의 데이터를 처리
* 관리되는 총 파티션 크기는 "(보관기간 + 미래 파티션 수 + 1)" 정도로 유지됨

## 파티셔닝 초기화 스크립트

개발 환경에서 모든 파티션을 제거하고 `PartitionManager`가 새로 초기화하도록 할 때 사용할 수 있는 스크립트입니다.

```sql
-- 모든 테이블의 파티션 제거 스크립트
-- 개발 환경에서 실행하여 PartitionManager가 새로 생성하도록 함

-- 1. auth_login_log 테이블 파티션 제거
ALTER TABLE auth_login_log
    REMOVE PARTITIONING;

-- 2. disbursement_error 테이블 파티션 제거
ALTER TABLE disbursement_error
    REMOVE PARTITIONING;

-- 3. disbursement_log 테이블 파티션 제거
ALTER TABLE disbursement_log
    REMOVE PARTITIONING;

-- 4. accounting_journal_entry 테이블 파티션 제거
ALTER TABLE accounting_journal_entry
    REMOVE PARTITIONING;

-- 5. 기본 파티션 키 유지를 위한 파티션 재설정
-- auth_login_log 테이블
ALTER TABLE auth_login_log
    PARTITION BY RANGE (TO_DAYS(created)) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

-- disbursement_error 테이블
ALTER TABLE disbursement_error
    PARTITION BY RANGE (TO_DAYS(created)) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

-- disbursement_log 테이블
ALTER TABLE disbursement_log
    PARTITION BY RANGE (TO_DAYS(created)) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

-- accounting_journal_entry 테이블
ALTER TABLE accounting_journal_entry
    PARTITION BY RANGE (TO_DAYS(created)) (
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

-- 주의: 이 스크립트는 모든 파티션을 제거합니다.
-- 실제 데이터가 있는 경우에는 백업 후 실행하세요.
```

> **주의**: 이 스크립트는 모든 파티션을 제거하므로 실제 데이터가 있는 프로덕션 환경에서는 반드시 백업 후 실행해야 합니다.