# DDD (Domain Driven Development) 구조

- app
- domain
- external
- global
- infra

# 주요 설정

1. [AppConfig](/src/main/kotlin/kr/pincoin/api/global/config/AppConfig.kt)
2. [AsyncConfig](/src/main/kotlin/kr/pincoin/api/global/config/AsyncConfig.kt)
3. [JpaConfig](/src/main/kotlin/kr/pincoin/api/global/config/JpaConfig.kt)
4. [ObjectMapperConfig](/src/main/kotlin/kr/pincoin/api/global/config/ObjectMapperConfig.kt)
5. [QueryDslConfig](/src/main/kotlin/kr/pincoin/api/global/config/QueryDslConfig.kt)
6. [SchedulerConfig](/src/main/kotlin/kr/pincoin/api/global/config/SchedulerConfig.kt)

# 주요 속성

- [application-local.yml](/src/main/resources/application-local.yml) 파일에 추가

```yaml
web-config:
  cors:
    allowed-origins: http://localhost:3000,http://localhost:8080
    allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
    allowed-headers: '*'
    max-age: 3600
```

[CorsProperties](/src/main/kotlin/kr/pincoin/api/global/properties/CorsProperties.kt)

# 엔드포인트와 역할 체계

| 엔드포인트        | 회원 유무   | 공개 유무                        |
|--------------|---------|------------------------------|
| /open/**     | 비회원     | 공개 API                       | 
| /auth/**     | 비회원     | 인증 관련 처리                     | 
| /member/**   | 비회원     | 인증 필요 생성 작업                  | 
| /my/**       | 로그인 사용자 | 개인 정보 관련 (user.id == userId) | 
| /admin/**    | 관리자     | 시스템 관리 (ROLE_ADMIN)          | 
| /webhooks/** | 외부 시스템  | 콜백                           | 

# global.response 패키지 추가

# 도메인, 엔티티와 리파지토리

## 도메인 모델

- 불변성 유지: val 변수
- private constructor 유지: of 팩토리 메소드
- copy 노출 방지: data class 대신 일반 class 사용, 명확한 변경 메소드 제공
- copy 헬퍼 메소드: 순수 도메인 모델에서 차선책
- require(), validate()의 적절한 사용으로 도메인 정합성 유지

## 엔티티

- private constructor
- 불변 데이터 val 변수
- of 팩토리 메소드
- 매핑을 사용하지 않음
    - on 절에서 ID 기반으로 조인 = 명시적인 조인 조건으로 쿼리 의도가 더 명확
    - JPA 지연로딩 관련 이슈 회피
    - 필요한 컬럼만 선택적으로 가져오므로 성능 최적화 가능

## 매퍼

- 확장 함수로 구현
- 절대 비즈니스 로직 포함하지 않음
- 지연 로딩 방지 mapper 필요

## DDD에서 연관관계 매핑 사용하지 않기

1. Aggregate 경계가 더 명확해집니다:

- ID 참조만 사용하면 Aggregate Root 간의 경계가 자연스럽게 분리됨
- 다른 Aggregate를 수정할 때 의도치 않은 변경이 전파되는 것을 방지

2. 성능상 이점:

- 필요한 데이터만 정확히 조회 가능
- N+1 문제나 지연로딩 관련 이슈를 원천적으로 방지
- 불필요한 Join을 피할 수 있음

3. 유지보수성 향상:

- 도메인 모델이 단순해지고 이해하기 쉬워짐
- JPA 프록시나 LazyInitializationException 같은 기술적 이슈에서 자유로워짐
- 테스트 작성이 더 쉬워짐 (Mock 객체 생성이 단순)

4. 트랜잭션 경계 관리가 쉬워집니다:

- 각 Aggregate별로 독립적인 트랜잭션 관리 가능
- 동시성 이슈 처리가 더 명확해짐
