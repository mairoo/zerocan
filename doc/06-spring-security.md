# 주요 파일

## 최소 설정

- [SecurityConfig](/src/main/kotlin/kr/pincoin/api/global/config/SecurityConfig.kt)
- [ApiAccessDeniedHandler](/src/main/kotlin/kr/pincoin/api/global/security/handler/ApiAccessDeniedHandler.kt)
- [ApiAuthenticationEntryPoint](/src/main/kotlin/kr/pincoin/api/global/security/handler/ApiAuthenticationEntryPoint.kt)

```kotlin
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val corsProperties: CorsProperties,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // 순서 중요: 1. 기본 설정 → 2. OAuth2 설정 (조건부) → 3. 권한 설정 → 4. 필터 → 5. 예외 처리 → 빌드

        // 1. 기본 설정
        val httpSecurity = http
            .cors { cors ->
                // CORS(Cross-Origin Resource Sharing) 설정
                cors.configurationSource(corsConfigurationSource())
            }
            .headers { headers ->
                // 기본 보안 헤더를 비활성화하고 필요한 것만 직접 설정
                headers.defaultsDisabled()

                // HTTPS 보안 정책 설정
                headers.httpStrictTransportSecurity { hstsConfig ->
                    hstsConfig
                        .includeSubDomains(true) // 서브도메인에도 HTTPS 적용
                        .maxAgeInSeconds(31536000) // HSTS 유효기간 1년
                        .preload(true) // 브라우저의 HSTS 프리로드 목록에 포함
                }

                // Content-Type 헤더를 브라우저가 임의로 변경하는 것을 방지
                headers.contentTypeOptions { }
                // 브라우저 캐시 제어 헤더 설정
                headers.cacheControl { }
            }
            .csrf { it.disable() } // CSRF 보안 비활성화 (REST API이므로 불필요)
            .formLogin { it.disable() } // 폼 로그인 비활성화
            .httpBasic { it.disable() } // HTTP Basic 인증 비활성화
            .rememberMe { it.disable() } // Remember-Me 기능 비활성화
            .anonymous { it.disable() } // 익명 사용자 기능 비활성화
            .sessionManagement { session ->
                // JWT 사용을 위한 세션리스 정책 설정
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
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
            // 5. 인증/인가 예외 처리
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }
            .build()

        return httpSecurity
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            exposedHeaders = listOf("Set-Cookie") // 클라이언트에 노출할 헤더
            allowCredentials = true // 인증 정보 포함 허용

            // allowedOrigins와 allowedOriginPatterns는 동시 사용 불가
            // 와일드카드 패턴 허용
            allowedOriginPatterns = corsProperties.allowedOrigins
                .split(",")
                .map { it.trim() }
                .map { origin ->
                    if (origin.contains("*")) origin else origin
                }
            allowedMethods = corsProperties.allowedMethods.split(",") // 허용할 HTTP 메서드
            allowedHeaders = corsProperties.allowedHeaders.split(",") // 허용할 헤더
            maxAge = corsProperties.maxAge // 프리플라이트 요청의 캐시 시간
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
```

## Keycloak OAuth2 설정

## 커스텀 필터 추가
