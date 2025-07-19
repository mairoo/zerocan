package kr.pincoin.api.global.utils

import jakarta.servlet.http.HttpServletRequest

/** 클라이언트 요청 정보를 추출하는 유틸리티 클래스 */
object ClientUtils {
    private const val USER_AGENT_HEADER = "User-Agent"
    private const val ACCEPT_LANGUAGE_HEADER = "Accept-Language"

    /**
     * HttpServletRequest에서 클라이언트 정보를 추출
     *
     * @param request HTTP 요청
     * @return 클라이언트 정보를 포함한 ClientInfo 객체
     */
    fun getClientInfo(request: HttpServletRequest): ClientInfo =
        ClientInfo(
            userAgent = request.getHeader(USER_AGENT_HEADER).orEmpty(),
            acceptLanguage = request.getHeader(ACCEPT_LANGUAGE_HEADER).orEmpty(),
            ipAddress = IpUtils.getClientIp(request)
        )

    /** 클라이언트 요청 정보를 담는 데이터 클래스 */
    data class ClientInfo(
        val userAgent: String,
        val acceptLanguage: String,
        val ipAddress: String
    )
}