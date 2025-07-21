package kr.pincoin.api.app.user.admin.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/users")
class AdminUserController {
    @GetMapping("/test")
    @PreAuthorize("isAuthenticated()")
    fun testAuthentication(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "인증된 사용자만 접근 가능합니다.",
                "status" to "success"
            )
        )
    }

    @GetMapping("/test1")
    @PreAuthorize("hasRole('ADMIN')")
    fun testAuthentication1(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "message" to "관리자만 접근 가능합니다.",
                "status" to "success"
            )
        )
    }
}