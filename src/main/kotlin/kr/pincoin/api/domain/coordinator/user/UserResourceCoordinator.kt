package kr.pincoin.api.domain.coordinator.user

import kr.pincoin.api.domain.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserResourceCoordinator(
    private val userService: UserService,
) {
}