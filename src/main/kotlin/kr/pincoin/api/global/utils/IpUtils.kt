package kr.pincoin.api.global.utils

import jakarta.servlet.http.HttpServletRequest

object IpUtils {
    private val IP_HEADERS = arrayOf(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_CLIENT_IP",
        "X-Real-IP", // Nginx
        "CF-Connecting-IP", // Cloudflare
    )

    fun getClientIp(request: HttpServletRequest): String =
        IP_HEADERS.firstNotNullOfOrNull { header ->
            val ip = request.getHeader(header)

            if (!ip.isNullOrEmpty() && !ip.equals("unknown", ignoreCase = true)) {
                // X-Forwarded-For 헤더는 콤마로 구분된 여러 IP를 포함할 수 있으므로 첫 번째 IP를 추출
                if (header == "X-Forwarded-For" && ip.contains(",")) {
                    ip.split(",")[0].trim()
                } else {
                    ip
                }
            } else {
                null
            }
        } ?: request.remoteAddr
}