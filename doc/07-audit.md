# 임베디드 타입

- [AuditorFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/AuditorFields.kt)
    - `created_by`
    - `modified_by`
  - `@EnableJpaAuditing`
- [DateTimeFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/DateTimeFields.kt)
    - `created_at`
    - `modified_at`
    - `@EnableJpaAuditing(auditorAwareRef = "userAuditorAware")`
- [RemovalFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/RemovalFields.kt)
    - `is_removed`

# 날짜 감사

- `domain.audit` 패키지
- `infra.audit` 패키지

## 예시 엔티티

```kotlin
@Entity
@Table(name = "user")
@EntityListeners(AuditEntityListener::class) // (1)
class UserEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @Column(name = "name")
    val name: String,

    // (2)
    @Embedded
    val dateTimeFields: DateTimeFields = DateTimeFields(),

    // (3)
    @Embedded
    val removalFields: RemovalFields = RemovalFields(),
) : Auditable { // (4)
    
    // (5)
    @Transient
    override var originState: Map<String, String?> =
        mapOf()

    // (6)
    override fun getState(): Map<String, String?> =
        mapOf(
            "id" to id?.toString(),
            "name" to name,
            "isRemoved" to removalFields.isRemoved.toString()
        )

    // (7)
    override fun getEntityType(): String = "User"

    // (8)
    override fun getEntityId(): String = id?.toString() ?: ""

    companion object {
        fun of(
            id: Long? = null,
            isRemoved: Boolean = false,
            name: String,
        ) = UserEntity(
            id = id,
            removalFields = RemovalFields().apply {
                this.isRemoved = isRemoved
            },
            name = name,
        )
    }
}
```

# 변경 수정 감사: `UserAuditorAware` 지원

```kotlin
@Component
class UserAuditorAware : AuditorAware<Long> {
  override fun getCurrentAuditor(
  ): Optional<Long> =
    SecurityContextHolder
      .getContext()
      .authentication
      ?.takeIf { it.isAuthenticated }
      ?.principal
      ?.let { principal ->
        when (principal) {
          is UserDetailsAdapter -> Optional.of(principal.user.id ?: -1L)
          else -> Optional.empty()
        }
      } ?: Optional.empty()
}
```

# JPA 기반 엔티티 변경 감사(Audit) 시스템 구현 방식

## 1. 개요

이 문서는 Firmbank 시스템의 엔티티 변경 감사(Audit) 시스템에 대한 설명입니다. 이 시스템은 JPA 엔티티의 생성, 수정, 삭제 작업을 추적하고 기록하는 메커니즘을 제공합니다.

## 2. 핵심 컴포넌트

### 2.1 인터페이스 및 모델

- **`Auditable` 인터페이스**: 감사 기능을 필요로 하는 엔티티가 구현해야 하는 인터페이스
- **`AuditChange`**: 필드 변경 사항을 나타내는 클래스
- **`AuditLog`**: 감사 로그 엔트리를 나타내는 클래스
- **`AuditType`**: 감사 이벤트 유형을 정의하는 열거형 (CREATE, UPDATE, DELETE)

### 2.2 핸들러 및 서비스

- **`AuditEntityListener`**: JPA 엔티티 이벤트를 감지하는 리스너
- **`AuditLogger` 인터페이스**: 감사 로깅 기능을 정의
- **`AuditLogEventHandler`**: 감사 로그 이벤트를 처리하는 컴포넌트
- **`UserAuditorAware`**: 현재 인증된 사용자 정보를 제공하는 컴포넌트

### 2.3 이벤트 및 저장소

- **`AuditLogEvent`**: 감사 로그 이벤트 데이터 클래스
- **`AuditLogRepository`**: 감사 로그를 저장 및 조회하는 저장소 인터페이스

### 2.4 설정

- **`JpaConfig`**: JPA Auditing 기능 활성화 및 설정

## 3. 감사 프로세스 작동 방식

### 3.1 엔티티 준비

감사 대상 엔티티는 다음과 같이 준비해야 합니다:

1. `Auditable` 인터페이스 구현
2. `@EntityListeners(AuditEntityListener::class)` 애노테이션 추가
3. 엔티티 상태 정보를 제공하는 메서드 구현:
  - `getState()`: 현재 상태 반환
  - `getEntityType()`: 엔티티 유형 반환
  - `getEntityId()`: 엔티티 ID 반환

### 3.2 엔티티 수명 주기 이벤트 처리

`AuditEntityListener`는 다음 JPA 수명 주기 이벤트를 처리합니다:

1. **`@PostLoad`**: 엔티티가 로드될 때 원본 상태 저장
2. **`@PostPersist`**: 엔티티가 생성된 후 생성 감사 로그 생성
3. **`@PostUpdate`**: 엔티티가 업데이트된 후 변경 감사 로그 생성
4. **`@PostRemove`**: 엔티티가 삭제된 후 삭제 감사 로그 생성

### 3.3 상태 비교 및 변경 추적

1. 엔티티가 로드될 때 원본 상태(`originState`)가 저장됨
2. 엔티티가 변경될 때 현재 상태(`getState()`)와 원본 상태를 비교
3. 차이점을 감지하여 `AuditChange` 객체로 변환

### 3.4 감사 로그 저장

감사 로그는 다음 프로세스를 통해 저장됩니다:

1. `AuditLogger` 구현체가 변경 사항을 감지
2. `AuditLogEvent` 생성 및 발행
3. `AuditLogEventHandler`가 이벤트를 수신
4. 새로운 트랜잭션에서 `AuditLog` 저장(`@Transactional(propagation = Propagation.REQUIRES_NEW)`)

## 4. 코드 예제 분석

### 4.1 감사 대상 엔티티 구현 예제 (`GroupEntity`)

```kotlin
@Entity
@Table(name = "auth_group")
@EntityListeners(AuditEntityListener::class)
class GroupEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    // 필드들...

) : Auditable {
    @Transient
    override var originState: Map<String, String?> = mapOf()

    override fun getState(): Map<String, String?> =
        mapOf(
            "id" to id.toString(),
            "name" to name,
            "description" to description,
            // 다른 필드들...
        )

    override fun getEntityType(): String = "Group"

    override fun getEntityId(): String = id?.toString() ?: ""

    // 기타 메서드들...
}
```

### 4.2 감사 로그 이벤트 처리 예제

```kotlin
@Component
class AuditLogEventHandler(
    private val auditLogRepository: AuditLogRepository,
) {
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleAuditLogEvent(event: AuditLogEvent) {
        auditLogRepository.save(
            AuditLog.of(
                entityType = event.entityType,
                entityId = event.entityId,
                type = event.type,
                origin = event.origin,
                changed = event.changed
            )
        )
    }
}
```

## 5. Spring Data JPA Auditing 통합

### 5.1 JPA Auditing 설정

`JpaConfig` 클래스는 Spring Data JPA의 Auditing 기능을 활성화합니다:

```kotlin
@Configuration
@EnableJpaAuditing(auditorAwareRef = "userAuditorAware") // created, modified 필드 자동 관리
class JpaConfig
```

### 5.2 사용자 감사 정보 제공자

`UserAuditorAware` 클래스는 현재 인증된 사용자의 ID를 자동으로 감사 필드에 설정하는 역할을 합니다:

```kotlin
@Component
class UserAuditorAware : AuditorAware<Long> {
    override fun getCurrentAuditor(): Optional<Long> =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal
            ?.let { principal ->
                when (principal) {
                    is UserDetailsAdapter -> Optional.of(principal.user.id ?: -1L)
                    else -> Optional.empty()
                }
            } ?: Optional.empty()
}
```

이 클래스는 Spring Security의 `SecurityContextHolder`를 활용하여 현재 인증된 사용자 정보를 조회하고, 이를 엔티티의 `createdBy` 및 `modifiedBy` 필드에 자동으로
설정합니다.

## 6. 주요 특징 및 장점

1. **분리된 트랜잭션**: 감사 로그 저장은 새로운 트랜잭션(`REQUIRES_NEW`)에서 실행되어 주 트랜잭션 실패와 분리됨
2. **이벤트 기반 아키텍처**: Spring의 이벤트 메커니즘을 활용하여 느슨한 결합 유지
3. **JPA 수명 주기 통합**: JPA 수명 주기 이벤트를 활용하여 자동 감사 기록
4. **필드 단위 변경 추적**: 개별 필드 변경을 추적하여 상세한 감사 정보 제공
5. **유연한 확장성**: 새로운 엔티티에 감사 기능을 쉽게 추가 가능
6. **자동 작성자 추적**: Spring Security와 통합되어 생성/수정한 사용자 정보 자동 기록

## 7. 구현 시 고려사항

1. **성능 영향**: 각 엔티티 변경마다 추가 데이터베이스 작업이 발생함
2. **저장 공간**: 감사 로그는 시간이 지남에 따라 많은 저장 공간을 차지할 수 있음
3. **민감한 데이터**: 감사 로그에 민감한 정보가 포함될 수 있으므로 보안 고려 필요
4. **인증 컨텍스트 의존성**: `UserAuditorAware`는 Spring Security 인증 컨텍스트에 의존하므로 인증이 없는 환경에서는 대체 전략 필요

## 8. 결론

JPA 기반 엔티티 감사 시스템은 시스템 변경 사항을 추적하고 기록하기 위한 강력하고 유연한 방법을 제공합니다. 이 시스템은 규제 준수, 보안 감사, 문제 해결 및 사용자 활동 추적에 필수적인 기능입니다.