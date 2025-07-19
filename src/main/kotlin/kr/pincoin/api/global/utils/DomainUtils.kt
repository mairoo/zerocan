package kr.pincoin.api.global.utils

import jakarta.servlet.http.HttpServletRequest

/**
 * 요청 도메인 처리를 위한 유틸리티 클래스
 */
object DomainUtils {
    private const val X_FORWARDED_HOST = "X-Forwarded-Host"
    private const val HOST = "Host"

    /**
     * 실제 클라이언트가 접속한 도메인을 추출합니다.
     * 프록시 환경에서는 X-Forwarded-Host 헤더를 우선적으로 사용합니다.
     *
     * @param request HTTP 요청
     * @return 실제 클라이언트가 접속한 도메인
     */
    fun getRequestDomain(request: HttpServletRequest): String {
        // 1. X-Forwarded-Host 헤더 확인 (프록시 환경)
        val forwardedHost = request.getHeader(X_FORWARDED_HOST)
        if (!forwardedHost.isNullOrBlank()) {
            // 콤마로 구분된 여러 값이 있을 경우 첫 번째 값을 사용
            return forwardedHost.split(',')[0].trim()
        }

        // 2. Host 헤더 확인
        val hostHeader = request.getHeader(HOST)
        if (!hostHeader.isNullOrBlank()) {
            return hostHeader
        }

        // 3. 위 두 헤더가 없는 경우 서버 이름 사용 (가장 마지막 대안)
        return request.serverName
    }

    /**
     * 도메인에서 포트 번호를 제거합니다.
     * 예: "example.com:8080" -> "example.com"
     */
    fun stripPort(domain: String): String {
        val colonIndex = domain.indexOf(':')
        return if (colonIndex > 0) domain.substring(0, colonIndex) else domain
    }
}