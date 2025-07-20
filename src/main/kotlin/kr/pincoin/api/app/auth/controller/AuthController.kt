package kr.pincoin.api.app.auth.controller

import jakarta.validation.Valid
import kr.pincoin.api.app.auth.request.SignUpRequest
import kr.pincoin.api.app.auth.service.AuthService
import kr.pincoin.api.app.user.common.response.UserResponse
import kr.pincoin.api.global.response.success.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    // sign-in

    // refresh

    // sign-out

    // sign-up
    /**
     * 회원 가입을 합니다.
     */
    @PostMapping("/sign-up")
    fun signUp(
        @Valid @RequestBody request: SignUpRequest,
    ): ResponseEntity<ApiResponse<UserResponse>> =
        authService.createUser(request)
            .let { UserResponse.from(it) }
            .let { ApiResponse.of(it) }
            .let { ResponseEntity.ok(it) }
}