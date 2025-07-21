package kr.pincoin.api.global.config

import kr.pincoin.api.external.auth.keycloak.properties.KeycloakProperties
import kr.pincoin.api.global.properties.CorsProperties
import kr.pincoin.api.global.security.handler.ApiAccessDeniedHandler
import kr.pincoin.api.global.security.handler.ApiAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val authenticationEntryPoint: ApiAuthenticationEntryPoint,
    private val accessDeniedHandler: ApiAccessDeniedHandler,
    private val corsProperties: CorsProperties,
    private val keycloakProperties: KeycloakProperties,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            // 1. 기본 설정
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .headers { headers ->
                headers.defaultsDisabled()
                headers.httpStrictTransportSecurity { hstsConfig ->
                    hstsConfig
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                        .preload(true)
                }
                headers.contentTypeOptions { }
                headers.cacheControl { }
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .rememberMe { it.disable() }
            .anonymous { it.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // 2. OAuth2 Resource Server 설정
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            // 3. 권한 설정
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
                        "/health"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            // 4. 예외 처리
            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            }
            .build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder
            .withJwkSetUri("${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/certs")
            .build()

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            // Keycloak에서 roles 클레임을 권한으로 변환
            setAuthoritiesClaimName("realm_access.roles")
            setAuthorityPrefix("ROLE_")
        }

        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter)
            // 사용자명으로 사용할 클레임 설정 (기본: sub)
            setPrincipalClaimName("preferred_username")
        }
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            exposedHeaders = listOf("Set-Cookie")
            allowCredentials = true

            allowedOriginPatterns = corsProperties.allowedOrigins
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            allowedMethods = corsProperties.allowedMethods
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            allowedHeaders = corsProperties.allowedHeaders
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            maxAge = corsProperties.maxAge
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}