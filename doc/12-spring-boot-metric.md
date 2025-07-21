# 스프링부트 애플리케이션 메트릭 설정

## `build.gradle.kts`에 의존성 추가

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

## `SecurityConfig`

```
    // 3. 엔드포인트별 권한 설정
    .authorizeHttpRequests { auth ->
        auth
            .requestMatchers(
                "/actuator/health",
                "/actuator/prometheus",
                "/actuator/info"
            ).permitAll()
            .requestMatchers("/actuator/**").denyAll()
            .requestMatchers(
                "/auth/**",
                "/oauth2/**",
                "/open/**",
                "/webhooks/**",
            ).permitAll()
            .anyRequest().authenticated()
    }
```

## `application-prod.yml`에 actuator 설정 추가

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name:api}
      environment: prod # 개발환경은 local
```