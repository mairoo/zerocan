# 스프링부트/코틀린 프로젝트 생성

## https://start.spring.io/

- 의존성 추가 없이 빈 프로젝트

- Project: Gradle- Kotlin
- Language: Kotlin
- Project Metadata
    - Group: kr.pincoin
    - Artifact: api
    - Name: api
    - Description
    - Package name: kr.pincoin.api
    - Packaging: Jar
    - Java: 24

## ignore 파일 추가

- [.gitignore](/.gitignore)
- [.dockerignore](/.dockerignore)

## git 저장소 연결

```
cd repo
git init
```

원격 저장소 생성 후 연결

# 빌드 설정

[build.gradle.kts](/build.gradle.kts) 수정

- QueryDSL 지원
- 서드파티 라이브러리 변경을 위한 상수 선언
