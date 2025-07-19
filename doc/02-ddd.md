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

# 엔드포인트와 역할 체계

| 엔드포인트        | 회원 유무   | 공개 유무                        |
|--------------|---------|------------------------------|
| /open/**     | 비회원     | 공개 API                       | 
| /auth/**     | 비회원     | 인증 관련 처리                     | 
| /member/**   | 비회원     | 인증 필요 생성 작업                  | 
| /my/**       | 로그인 사용자 | 개인 정보 관련 (user.id == userId) | 
| /admin/**    | 관리자     | 시스템 관리 (ROLE_ADMIN)          | 
| /webhooks/** | 외부 시스템  | 콜백                           | 
