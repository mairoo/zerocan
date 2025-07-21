# Resource Coordinator 명세

Resource Coordinator는 도메인 간 상호작용과 상태 관리를 처리하는 서비스 계층입니다. 각 Coordinator는 여러 서비스를 조합하여 일관된 비즈니스 트랜잭션을 관리합니다.

## Group Resource Coordinator

그룹과 관련된 리소스를 조정하는 서비스로, 그룹 상태 변경 시 관련 사용자 처리를 관리합니다.

### 주요 기능

#### 그룹 보류 처리

```
fun hold(groupId: Long): Group
```

1. 그룹에 속한 모든 활성 사용자 조회
2. 각 사용자에 대해 보류 처리 (UserResourceCoordinator 위임)
3. 그룹을 보류 상태로 변경

#### 그룹 정지 처리

```
fun suspend(groupId: Long): Group
```

1. 그룹에 속한 모든 활성 사용자 조회
2. 각 사용자에 대해 정지 처리 (UserResourceCoordinator 위임)
3. 그룹을 정지 상태로 변경

#### 그룹 소프트 삭제 처리

```
fun remove(groupId: Long): Group
```

1. 그룹에 속한 모든 활성 사용자 조회
2. 각 사용자에 대해 소프트 삭제 처리 (UserResourceCoordinator 위임)
3. 그룹 소프트 삭제 수행

## User Resource Coordinator

사용자와 관련된 리소스를 조정하는 서비스로, 사용자 상태 변경 시 관련 그룹 및 권한 처리를 관리합니다.

### 주요 기능

#### 사용자 생성

```
fun createUser(request: UserCreateRequest): User
```

1. 그룹 존재 확인 및 활성 사용자 수 증가
2. 사용자 생성

#### 사용자 그룹 변경

```
fun updateUserGroup(userId: Long, request: AdminUserGroupUpdateRequest): User
```

1. 사용자 조회
2. 사용자가 활성 상태인 경우에만 카운트 조정
    - 이전 그룹의 활성 사용자 수 감소
    - 새 그룹의 활성 사용자 수 증가
3. 사용자 그룹 업데이트

#### 사용자 보류 처리

```
fun hold(userId: Long): User
```

1. 사용자를 보류 상태로 변경
2. 사용자 비활성화에 따른 그룹 카운트 조정
3. 사용자가 소유하지 않은 계좌에 대한 권한 비활성화
4. 사용자가 소유한 계좌 조회
5. 사용자가 소유한 각 계좌에 대한 보류 처리 위임 (DisbursementAccountResourceCoordinator)

#### 사용자 정지 처리

```
fun suspend(userId: Long): User
```

1. 사용자를 정지 상태로 변경
2. 사용자 비활성화에 따른 그룹 카운트 조정
3. 사용자가 소유하지 않은 계좌에 대한 권한 비활성화
4. 사용자가 소유한 계좌 조회
5. 사용자가 소유한 각 계좌에 대한 정지 처리 위임 (DisbursementAccountResourceCoordinator)

#### 사용자 소프트 삭제 처리

```
fun remove(userId: Long): User
```

1. 사용자 조회
2. 사용자 소프트 삭제
3. 사용자가 활성 상태였을 때만 그룹의 활성 사용자 수 감소
4. 사용자가 소유하지 않은 계좌에 대한 권한 소프트 삭제
5. 사용자가 소유한 계좌 조회
6. 사용자가 소유한 각 계좌에 대한 소프트 삭제 처리 위임 (DisbursementAccountResourceCoordinator)

#### 사용자 활성화

```
fun activate(userId: Long): User
```

1. 사용자 활성화
2. 사용자의 그룹 활성 사용자 수 증가

## Disbursement Group Resource Coordinator

모계좌 그룹과 관련된 리소스를 조정하는 서비스로, 그룹 상태 변경 시 관련 모계좌 처리를 관리합니다.

### 주요 기능

#### 모계좌 그룹 보류 처리

```
fun hold(disbursementGroupId: Long): DisbursementGroup
```

1. 그룹에 속한 모든 활성 모계좌 조회
2. 각 모계좌에 대해 보류 처리 (DisbursementResourceCoordinator 위임)
3. 모계좌 그룹 보류 상태로 변경

#### 모계좌 그룹 정지 처리

```
fun suspend(disbursementGroupId: Long): DisbursementGroup
```

1. 그룹에 속한 모든 활성 모계좌 조회
2. 각 모계좌에 대해 정지 처리 (DisbursementResourceCoordinator 위임)
3. 모계좌 그룹 정지 상태로 변경

#### 모계좌 그룹 소프트 삭제 처리

```
fun remove(disbursementGroupId: Long): DisbursementGroup
```

1. 그룹에 속한 모든 모계좌 조회 (활성/비활성 모두)
2. 각 모계좌에 대해 소프트 삭제 처리 (DisbursementResourceCoordinator 위임)
3. 모계좌 그룹 소프트 삭제

## Disbursement Resource Coordinator

모계좌와 관련된 리소스를 조정하는 서비스로, 모계좌 상태 변경 시 관련 지급계좌 처리를 관리합니다.

### 주요 기능

#### 모계좌 보류 처리

```
fun hold(disbursementId: Long): Disbursement
```

1. 참조하는 지급계좌 목록 조회
2. 각 지급계좌 보류 처리 (DisbursementAccountResourceCoordinator 위임)
3. 모계좌 보류 상태로 변경

#### 모계좌 정지 처리

```
fun suspend(disbursementId: Long): Disbursement
```

1. 참조하는 지급계좌 목록 조회
2. 각 지급계좌 정지 처리 (DisbursementAccountResourceCoordinator 위임)
3. 모계좌 정지 상태로 변경

#### 모계좌 소프트 삭제 처리

```
fun remove(disbursementId: Long)
```

1. 참조하는 지급계좌 목록 조회
2. 각 지급계좌 소프트 삭제 처리 (DisbursementAccountResourceCoordinator 위임)
3. 모계좌 소프트 삭제 처리

#### 모계좌 수수료 업데이트

```
fun updateDisbursementFees(disbursementId: Long, request: DisbursementFeeUpdateRequest): Disbursement
```

1. 모계좌 수수료 업데이트
2. 연결된 지급계좌 목록 조회
3. 지급계좌 수수료 검사 및 조정:
    - 이체 수수료 검사 (모계좌 비용 > 지급계좌 수익)
    - 조회 수수료 검사 (모계좌 비용 > 지급계좌 수익)
    - 필요한 경우에만 지급계좌 수수료 업데이트

## Disbursement Account Resource Coordinator

지급계좌와 관련된 리소스를 조정하는 서비스로, 계좌 상태 변경 시 관련 권한 및 계정과목 처리를 관리합니다.

### 주요 기능

#### 지급계좌 생성

```
fun create(request: DisbursementAccountCreateRequest): DisbursementAccount
```

1. 기존 매핑 존재 여부 확인
2. 지급계좌 생성
3. 지급계좌 소유권한 생성
4. 지급계좌 접근권한 생성
5. 지급계좌 ApiKey 생성
6. 지급계좌 계정과목 일괄 생성 (자산/부채/수익/비용 계정)
7. 지급계좌의 금일 및 익일 마감 잔액 추가
8. 모계좌의 activeAccountCount 증가

#### 지급계좌 정보 업데이트

```
fun updateDisbursementAccount(disbursementAccountId: Long, request: DisbursementAccountUpdateRequest): DisbursementAccount
```

- 업데이트 필드: title, description

#### 지급계좌 수수료 업데이트

```
fun updateDisbursementAccountFees(disbursementAccountId: Long, request: DisbursementAccountFeeUpdateRequest): DisbursementAccount
```

- 업데이트 필드: 이체수수료 수익, 조회수수료 수익

#### 지급계좌 간 이체 수행

```
fun transferBetweenDisbursementAccounts(sourceDisbursementAccountId: Long, request: DisbursementAccountTransferRequest): TransferResponse
```

1. 이체 요청 수행
2. 성공 시:
    - 입금 계좌에 대한 입금 분개 생성
    - 출금 계좌 잔액 재계산
    - 입금 계좌 잔액 재계산
3. 실패 시:
    - 로깅 및 필요한 조치

#### 지급계좌 보류 처리

```
fun hold(disbursementAccountId: Long): DisbursementAccount
```

1. 공통 비활성화 처리 수행:
    - 연관된 접근권한 비활성화
    - 연관된 API 키 비활성화
    - 연관된 소유권한 비활성화
    - 모계좌의 activeAccountCount 감소
2. 카운터 초기화:
    - resetActiveOwnershipCount()
    - resetActiveAccessCount()
    - resetActiveApiKeyCount()
3. 계좌 보류 상태로 변경

#### 지급계좌 정지 처리

```
fun suspend(disbursementAccountId: Long): DisbursementAccount
```

1. 공통 비활성화 처리 수행:
    - 연관된 접근권한 비활성화
    - 연관된 API 키 비활성화
    - 연관된 소유권한 비활성화
    - 모계좌의 activeAccountCount 감소
2. 카운터 초기화:
    - resetActiveOwnershipCount()
    - resetActiveAccessCount()
    - resetActiveApiKeyCount()
3. 계좌 정지 상태로 변경

#### 지급계좌 소프트 삭제 처리

```
fun remove(disbursementAccountId: Long): DisbursementAccount
```

1. 계정 상태 확인을 위해 삭제 전 조회
2. 연관된 권한들 소프트 삭제:
    - 접근권한 소프트 삭제
    - API 키 소프트 삭제
    - 소유권한 소프트 삭제
3. 지급계좌 소프트 삭제 및 카운터 초기화
4. 계정이 활성 상태였을 때만 모계좌의 activeAccountCount 감소

#### 지급계좌 활성화

```
fun activate(disbursementAccountId: Long): DisbursementAccount
```

1. 계좌 활성화
2. 모계좌의 activeAccountCount 증가

## Disbursement User Ownership Resource Coordinator

지급계좌 소유권한과 관련된 리소스를 조정하는 서비스입니다.

### 주요 기능

#### 소유권한 활성화

```
fun activateOwnership(ownershipId: Long): DisbursementUserOwnership
```

1. 소유권한 활성화
2. 소유자의 접근권한 확인
3. 접근권한이 비활성화 상태인지 확인
4. 지급계좌 업데이트:
    - activeOwnershipCount 증가
    - 필요시 activeAccessCount 증가
5. 접근권한 활성화 (필요한 경우)

#### 소유권한 비활성화

```
fun deactivateOwnership(ownershipId: Long): DisbursementUserOwnership
```

1. 소유권한 비활성화
2. 해당 소유권한에 연결된 지급계좌의 모든 활성 상태 접근권한 조회
3. 조회된 모든 접근권한 비활성화 (소유자 및 다른 사용자의 접근권한 모두 포함)
4. 지급계좌 상태 업데이트:
    - activeOwnershipCount 감소
    - activeAccessCount 초기화

#### 계좌 소유권한 일괄 비활성화

```
fun deactivateOwnershipsForDisbursementAccount(disbursementAccountId: Long)
```

1. 소유권한 비활성화
2. 접근권한 비활성화
3. 지급계좌 카운터 초기화:
    - resetActiveOwnershipCount()
    - resetActiveAccessCount()

## Disbursement User Access Resource Coordinator

지급계좌 접근권한과 관련된 리소스를 조정하는 서비스입니다.

### 주요 기능

#### 접근권한 생성

```
fun createAccess(request: DisbursementUserAccessCreateRequest): DisbursementUserAccess
```

1. 기존 매핑 존재 여부 확인
2. 접근권한 생성
3. 지급계좌의 activeAccessCount 증가

#### 접근권한 업데이트

```
fun updateAccessPermissions(accessId: Long, request: DisbursementUserPermissionUpdateRequest): DisbursementUserAccess
```

- 업데이트: userAccess.canTransfer, userAccess.canInquire

#### 접근권한 활성화

```
fun activateAccess(accessId: Long): DisbursementUserAccess
```

1. 접근권한 활성화
2. 지급계좌의 activeAccessCount 증가

#### 접근권한 비활성화

```
fun deactivateAccess(accessId: Long): DisbursementUserAccess
```

1. 소유권이 존재하는지 확인 (존재시 비활성화 불가)
2. 접근권한 비활성화
3. 지급계좌의 activeAccessCount 감소

#### 계좌 접근권한 일괄 비활성화

```
fun deactivateAccessesForDisbursementAccount(disbursementAccountId: Long)
```

1. 접근권한 비활성화
2. 지급계좌의 activeAccessCount 초기화 (활성 상태인 접근권한이 있었던 경우에만)

## API Key Resource Coordinator

API 키와 관련된 리소스를 조정하는 서비스입니다.

### 주요 기능

#### API 키 활성화

```
fun activateApiKey(id: Long): ApiKey
```

1. API 키 활성화
2. 도메인 모델에서 활성화 시 새로운 시크릿 키 생성
3. 지급계좌의 activeApiKeyCount 증가

#### API 키 비활성화

```
fun deactivateApiKey(id: Long): ApiKey
```

1. API 키 비활성화
2. 지급계좌의 activeApiKeyCount 감소

#### 계좌 API 키 일괄 비활성화

```
fun deactivateApiKeysForDisbursementAccount(disbursementAccountId: Long)
```

1. API 키 조회
2. API 키 비활성화
3. 지급계좌의 activeApiKeyCount 초기화

#### API 키 소프트 삭제

```
fun removeApiKey(id: Long): ApiKey
```

1. API 키 조회 및 상태 확인
2. API 키 소프트 삭제
3. 이미 비활성화된 API 키에 대해서는 수량 차감 없음
4. 활성 상태였던 경우 지급계좌의 activeApiKeyCount 감소

#### 계좌 API 키 일괄 소프트 삭제

```
fun removeApiKeysForDisbursementAccount(disbursementAccountId: Long)
```

1. API 키 목록 조회
2. API 키 소프트 삭제
3. 활성 키가 있었던 경우 지급계좌의 activeApiKeyCount 초기화