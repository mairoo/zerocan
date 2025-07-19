package kr.pincoin.api.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing
// @EnableJpaAuditing(auditorAwareRef = "userAuditorAware")
class JpaConfig