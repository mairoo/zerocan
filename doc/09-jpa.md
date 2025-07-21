# 엔티티와 도메인 모델

- [User](/src/main/kotlin/kr/pincoin/api/domain/user/model/User.kt): User 도메인 모델
- [UserEntity](/src/main/kotlin/kr/pincoin/api/infra/user/entity/UserEntity.kt): User 엔티티
- [UserMapper](/src/main/kotlin/kr/pincoin/api/infra/user/mapper/UserMapper.kt): User 도메인 - 엔티티 매퍼 (코틀린 확장 함수 기능)

# repository 파일 구조

- [UserRepository](/src/main/kotlin/kr/pincoin/api/domain/user/repository/UserRepository.kt): 인프라 종속 없는 순수 코틀린 인터페이스
- [UserRepositoryImpl](/src/main/kotlin/kr/pincoin/api/infra/user/repository/UserRepositoryImpl.kt): 실제 RDBMS 접근 구현체
- [UserJpaRepository](/src/main/kotlin/kr/pincoin/api/infra/user/repository/UserJpaRepository.kt): 스프링 데이터 JPA 인터페이스
- [UserQueryRepository](/src/main/kotlin/kr/pincoin/api/infra/user/repository/UserQueryRepository.kt): QueryDSL 인터페이스
- [UserQueryRepositoryImpl](/src/main/kotlin/kr/pincoin/api/infra/user/repository/UserQueryRepositoryImpl.kt): QueryDSL
  구현체
- [UserJdbcRepository](/src/main/kotlin/kr/pincoin/api/infra/user/repository/UserJdbcRepository.kt): JDBC 템플릿 배치작업 구현체

# User - Role 특징

- User는 여러 Role을 가질 수 있다.
- Role 이름 자체는 enum 문자열이다.
- UserRepositoryImpl 내부에 Role을 같이 저장하는 게 숨겨져 있다.

# 상속 구조

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