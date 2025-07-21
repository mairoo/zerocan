### 타임아웃 재시도 처리 흐름

1. **타임아웃 발생**
    - `DoznTransferService.handleTimeout` 메소드에서 `DoznTransferTimeoutEvent` 발행
    - 거래 ID, 요청정보, 시퀀스 번호 등 포함

2. **이벤트 핸들링**
    - `DoznTransferTimeoutEventHandler`가 트랜잭션 커밋 후 이벤트 수신
    - 재시도 간격: 5분 1초, 10분 1초, 20분 1초 (총 3회)

3. **시간 간격 관리**
    - `shouldRescheduleForLaterExecution`에서 적절한 대기 시간 확인
    - 충분한 시간 경과 전이면 `taskScheduler`로 나중에 재실행 예약

4. **거래 결과 조회**
    - 적절한 시간 경과 후 `doznTransferCheckService.transferCheck` 호출
    - 은행 API로 실제 거래 상태 확인

5. **성공 처리**
    - 거래 확인 성공 시 `TransferCompletedEvent` 발행
    - 로그 업데이트 및 오류 기록을 성공 상태로 변경

6. **실패 처리**
    - 최대 재시도 횟수 초과하거나 조회 실패 시 실패로 처리
    - 오류 기록을 최종 실패 상태로 변경

# 구현 비교

## `@EventListener`를 사용한 경우

```kotlin
@Async
@EventListener
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun handleTransferTimeoutEvent(event: DoznTransferTimeoutEvent) {
```

- 이 구현에서는 이벤트가 발행되면 바로 리스너가 호출된다.
- 트랜잭션 상태와 무관하게 이벤트 처리가 되므로 `disbursementErrorService.saveErrorAsync()`가 완료되기 전에도 이벤트 리스너가 실행될 수
  있다.
- 이벤트를 발행한 원래 트랜잭션과는 완전히 독립적인 새로운 트랜잭션이 생성된다.

`DoznTransferService.handleTimeout` 메소드 실행 흐름은 다음과 같습니다:

1. `disbursementErrorService.saveErrorAsync()`를 호출하여 에러 로그를 저장합니다.
2. `eventPublisher.publishEvent()`로 `DoznTransferTimeoutEvent`를 발행합니다.
3. `return TransferResponse.Error`로 메소드를 종료합니다.

이 메소드가 종료되면 해당 트랜잭션이 커밋됩니다. 따라서 에러 로그는 데이터베이스에 저장되고 커밋이 완료됩니다.

이벤트 핸들러 `handleTransferTimeoutEvent`는 `@Async`로 인해 별도 스레드에서 실행되며,
`@Transactional(propagation = Propagation.REQUIRES_NEW)`로 인해 완전히 새로운 트랜잭션이 시작됩니다.

`shouldRescheduleForLaterExecution` 메소드에서 시간 대기 스케줄링이 발생할 때는 이미 원래 트랜잭션이 커밋되었으므로, 다음에 이벤트 핸들러가 실행될 때
`disbursementErrorService.findDisbursementError()`를 호출하면 커밋된 에러 로그를 정상적으로 조회할 수 있습니다.

즉, 이벤트 발행 측의 트랜잭션과 이벤트 핸들러의 트랜잭션이 완전히 분리되어 있어, 핸들러가 실행될 때는 데이터베이스에 에러 로그가 이미 저장되어 있으므로 정상적으로 조회 및 상태
변경이 가능합니다.

트랜잭션 경계를 분석해보겠습니다:

1. **시작점: ApiDisbursementController.createTransfer**
    - 컨트롤러 메소드에 별도의 `@Transactional` 어노테이션이 보이지 않습니다.

2. **DisbursementFacade.transfer**
    - 메소드에 `@Transactional` 어노테이션이 없으나, 서비스 클래스 수준에서 선언되었을 가능성이 있습니다.
    - 이 메소드 내에서 `validateAndGetFirmBank` 및 `findBankingService`는 읽기 작업으로 보입니다.

3. **DoznDisbursementService.transfer**
    - `suspend` 메소드이며, 이 클래스나 메소드에 `@Transactional` 선언이 있을 가능성이 있습니다.

4. **DoznTransferService.transfer**
    - `@Transactional` 어노테이션이 메소드에 보이며, 이 메소드 내에서 주요 트랜잭션이 시작될 가능성이 높습니다.
    - 코드를 보면 `getNextSequence`로 DB 읽기를 수행합니다.

예상되는 트랜잭션 경계:

1. **성공 시나리오**:
    - `DoznTransferService.transfer`에서 트랜잭션 시작
    - API 호출 성공 후 `disbursementLogService.saveLogAsync()` 실행
    - `TransferCompletedEvent` 이벤트 발행
    - `TransferResponse.Success` 반환 및 트랜잭션 커밋

2. **실패 시나리오**:
    - `DoznTransferService.transfer`에서 트랜잭션 시작
    - API 호출 실패 후 `disbursementErrorService.saveErrorAsync()` 실행
    - `TransferResponse.Error` 반환 및 트랜잭션 커밋

3. **타임아웃 시나리오**:
    - `DoznTransferService.transfer`에서 트랜잭션 시작
    - 타임아웃 발생 후 `disbursementErrorService.saveErrorAsync()` 실행
    - `DoznTransferTimeoutEvent` 이벤트 발행
    - `TransferResponse.Error` 반환 및 트랜잭션 커밋

4. **이벤트 핸들러**:
    - `handleTransferTimeoutEvent`는 `@Async`와
      `@Transactional(propagation = Propagation.REQUIRES_NEW)`로 인해
    - 완전히 별도의 트랜잭션에서 실행됩니다
    - 이 트랜잭션은 원래 트랜잭션이 커밋된 후에 시작됩니다

여기서 중요한 점은 `suspend` 함수와 Spring의 트랜잭션 관리 사이의 상호작용입니다. 코루틴의 스레드 전환으로 인해 트랜잭션 컨텍스트가 유지되지 않을 수 있으므로, 실제
트랜잭션 경계는 코드의 구현 방식에 따라 달라질 수 있습니다.

## `@TransactionalEventListener`를 사용한 경우

```kotlin
@Async("taskExecutor")
@TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT,
    fallbackExecution = true,
)
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun handleTransferTimeoutEvent(event: DoznTransferTimeoutEvent) {
```

- 이 구현에서는 이벤트가 발행된 트랜잭션이 성공적으로 커밋된 후에만 리스너가 호출된다.
- `fallbackExecution = true` 설정으로 인해 트랜잭션 컨텍스트 외부에서 이벤트가 발행된 경우에도 실행은 가능하다.

코루틴 비동기 처리와 트랜잭션 관련하여:

1. `disbursementErrorService.saveErrorAsync()`가 비동기 코루틴으로 실행되면, 이 메서드의 호출이 반환된 시점과 실제 데이터가 저장되는 시점
   사이에 시간차가 발생할 수 있습니다.

2. Spring의 트랜잭션 관리는 주로 ThreadLocal을 기반으로 하므로, 코루틴이 스레드를 전환하면 트랜잭션 컨텍스트가 유실될 수 있습니다.

3. `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`을 사용할 때, 이벤트 발행이 트랜잭션 내에서
   이루어졌지만 코루틴의 스레드 전환으로 인해 트랜잭션 컨텍스트가 유실되면, Spring은 이 이벤트를 트랜잭션 외부에서 발행된 것으로 간주할 수 있습니다.

4. `fallbackExecution = true` 설정이 있으므로 트랜잭션 없이 발행된 이벤트도 처리는 됩니다만, 원래 의도했던 "트랜잭션이 성공적으로 커밋된 후에만 처리"라는
   보장은 없어질 수 있습니다.
