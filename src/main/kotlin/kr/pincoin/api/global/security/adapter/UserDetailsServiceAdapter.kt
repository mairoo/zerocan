package kr.pincoin.api.global.security.adapter

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.pincoin.api.domain.user.error.UserErrorCode
import kr.pincoin.api.domain.user.repository.UserRepository
import kr.pincoin.api.infra.user.repository.criteria.UserSearchCriteria
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true) // 조회 전용 트랜잭션 적용
class UserDetailsServiceAdapter(
    private val userRepository: UserRepository,
) : UserDetailsService {
    private val log = KotlinLogging.logger {}

    /**
     * `UserRepository`를 스프링시큐리티의 `UserDetailsService`로 변환하는 어댑터
     *
     * - 도메인 계층 UserRepository.findByEmail -> 어댑터(단방향 mapper) -> UserDetailsService.loadUserByUsername
     * - 메서드명과 반환 타입의 불일치 해결:
     *   - findByEmail -> loadUserByUsername
     *   - Optional<User> -> UserDetails
     */
    override fun loadUserByUsername(
        email: String,
    ): UserDetails =
        UserDetailsAdapter(
            userRepository.findUser(UserSearchCriteria(email = email, isActive = true))
                ?: throw UsernameNotFoundException(UserErrorCode.INVALID_CREDENTIALS.message)
                    .also { log.error { "이메일 없음: $email" } })
}