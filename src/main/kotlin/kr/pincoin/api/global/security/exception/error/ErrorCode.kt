package kr.pincoin.api.global.security.exception.error

import org.springframework.http.HttpStatus

interface ErrorCode {
    val status: HttpStatus
    val message: String
}