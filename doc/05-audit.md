# 임베디드 타입

- [AuditorFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/AuditorFields.kt)
    - `created_by`
    - `modified_by`
- [DateTimeFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/DateTimeFields.kt)
    - `created_at`
    - `modified_at`
- [RemovalFields](/src/main/kotlin/kr/pincoin/api/infra/common/jpa/RemovalFields.kt)
    - `is_removed`

# Audit 기능

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

# `UserAuditorAware` 지원

```kotlin
@Component
class UserAuditorAware : AuditorAware<Long> {
    override fun getCurrentAuditor(
    ): Optional<Long> = Optional.empty()
}
```