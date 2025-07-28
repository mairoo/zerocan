# 잔액의 무결성과 거래의 멱등성

```kotlin
/**
     * 거래를 처리하고 계좌 잔액을 업데이트합니다.
     * 낙관적 락과 유니크 제약으로 동시성 제어와 멱등성을 보장합니다.
     *
     * @param accountId 계좌 ID
     * @param amount 거래 금액 (출금은 음수, 입금은 양수)
     * @param txId 거래 ID (중복 요청 방지용)
     * @param note 거래 메모 (선택)
     * @return 생성된 거래 내역
     * @throws IllegalArgumentException 계좌가 존재하지 않는 경우
     * @throws DataIntegrityViolationException 중복된 txId인 경우
     * @throws ConcurrentModificationException 낙관적 락 충돌 발생한 경우
     */
    @Transactional
    suspend fun processTransaction(
        accountId: Long,
        amount: BigDecimal,
        txId: String,
        note: String? = null
    ): Transaction {
        // 1. 계좌 조회
        val account = accountRepository.findAccount(accountId)
            ?: throw IllegalArgumentException("계좌가 없습니다: $accountId")

        // 2. 계좌 업데이트 계산
        val updatedAccount = account.processTransaction(amount)

        // 3. 트랜잭션 생성
        val transaction = Transaction.of(
            amount = amount,
            balanceAfter = updatedAccount.balance,
            txId = txId,
            note = note,
            accountId = accountId,
        )

        // 4. 트랜잭션 저장 시도 (중복 txId 검사)
        val saved = transactionRepository.save(transaction)

        try {
            // 5. 트랜잭션이 성공적으로 저장된 후 계좌 업데이트
            accountRepository.save(updatedAccount)
        } catch (e: OptimisticLockingFailureException) {
            throw ConcurrentModificationException("계좌 변경 낙관적 락 충돌")
        }

        return saved
    }
```

# API 성공 후 DB 저장 실패 대응 전략

1. 보상 트랜잭션 패턴

장점:

- 즉시 일관성 유지
- 실패 상황 명확한 처리

단점:

- 복잡한 구현
- **외부 시스템 취소 API 필요**
- **보상 트랜잭션도 실패할 수 있음**: 수동 개입 필요

2. 아웃박스 패턴

장점:

- 구현이 단순
- 높은 신뢰성
- 재시도가 용이
- 메인 트랜잭션과 후속 처리 분리

단점:

- 즉시 일관성이 아닌 최종 일관성
- 추가 테이블/인프라 필요
- 이벤트 처리 지연 가능성

비즈니스 요구사항(즉각적인 응답이 필요한지, 일관성이 더 중요한지)에 따라 적절한 방식 선택

- 즉시 일관성이 필요하다면 -> 보상 트랜잭션
- 지연 처리가 허용된다면 -> 아웃박스 패턴
- 외부 시스템 취소 API가 없다면 -> 아웃박스 패턴
- 시스템 복잡도를 낮추고 싶다면 -> 아웃박스 패턴

일반적으로 금융 거래에서는

- 실제 돈과 관련된 핵심 거래는 보상 트랜잭션
- 부가적인 처리(알림, 통계 등)는 아웃박스 패턴

## 보상 트랜잭션 패턴

```kotlin
private suspend fun handleSuccessResponse(
    request: TransferRequest,
    response: TransferResponse
): TransferResponse {
    log.debug { "이체 성공 응답 처리" }

    return try {
        // 1. API 응답 결과를 임시 저장
        val apiResult = response

        try {
            // 2. DB 저장 시도
            repository.save(...)

            // 3. 성공 시 정상 응답 반환
            response
        } catch (e: Exception) {
            // 4. DB 저장 실패 시 보상 트랜잭션 실행
            try {
                // 이체 취소 API 호출
                cancelTransfer(request, apiResult)
                throw TransactionCompensatedException("DB 저장 실패로 이체가 취소되었습니다", e)
            } catch (compensateError: Exception) {
                // 보상 트랜잭션도 실패한 경우 - 수동 개입이 필요한 심각한 상황
                log.error(compensateError) { "보상 트랜잭션 실패 - 수동 처리 필요" }
                // 관리자 알림 발송
                notifyAdmin(request, apiResult, e, compensateError)
                throw SystemException("시스템 오류가 발생했습니다")
            }
        }
    } catch (e: Exception) {
        handleError(request, e)
    }
}
```

## 아웃박스 패턴

```kotlin
@Transactional
private suspend fun handleSuccessResponse(
    request: TransferRequest,
    response: TransferResponse
): TransferResponse {
    log.debug { "이체 성공 응답 처리" }

    return try {
        // 1. API 응답 결과를 outbox 테이블에 먼저 저장
        outboxRepository.save(
            TransferOutbox(
                request = request,
                response = response,
                status = OutboxStatus.PENDING
            )
        )

        try {
            // 2. 실제 비즈니스 DB 저장
            repository.save(...)

            // 3. outbox 상태 업데이트
            outboxRepository.updateStatus(request.id, OutboxStatus.COMPLETED)

            response
        } catch (e: Exception) {
            // 4. DB 저장 실패 시 outbox 상태 업데이트
            outboxRepository.updateStatus(request.id, OutboxStatus.FAILED)
            throw e
        }
    } catch (e: Exception) {
        handleError(request, e)
    }
}

// 별도 배치 프로세스
@Scheduled(fixedDelay = 5000)
fun processFailedTransfers() {
    // FAILED 상태의 outbox 엔트리들을 조회하여
    // 보상 트랜잭션 실행 또는 재시도 로직 구현
}
```

## 이벤트 소싱 saga 패턴
