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
