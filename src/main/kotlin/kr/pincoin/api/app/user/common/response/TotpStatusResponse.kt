package kr.pincoin.api.app.user.common.response

/**
 * TOTP 2FA 상태 응답
 *
 * 사용자의 현재 2FA 활성화 상태와 추가 정보를 제공합니다.
 */
data class TotpStatusResponse(
    /**
     * 2FA 활성화 여부
     * true: 2FA가 활성화되어 있음 (로그인 시 OTP 코드 필요)
     * false: 2FA가 비활성화되어 있음 (이메일/비밀번호만으로 로그인 가능)
     */
    val enabled: Boolean,

    /**
     * 2FA 설정이 강제되었는지 여부
     * true: 관리자가 2FA 설정을 강제함 (다음 로그인 시 설정 화면 표시)
     * false: 강제 설정 없음
     */
    val setupRequired: Boolean = false,

    /**
     * 마지막 2FA 사용 시간 (밀리초)
     * null: 2FA를 사용한 적이 없거나 비활성화 상태
     */
    val lastUsedAt: Long? = null,

    /**
     * 백업 코드 개수
     * 디바이스 분실 시 사용할 수 있는 일회용 백업 코드의 남은 개수
     */
    val backupCodesCount: Int = 0,
) {
    companion object {
        /**
         * 2FA 비활성화 상태
         */
        fun disabled() = TotpStatusResponse(
            enabled = false,
            setupRequired = false,
            lastUsedAt = null,
            backupCodesCount = 0,
        )

        /**
         * 2FA 활성화 상태
         */
        fun enabled(
            lastUsedAt: Long? = null,
            backupCodesCount: Int = 0
        ) = TotpStatusResponse(
            enabled = true,
            setupRequired = false,
            lastUsedAt = lastUsedAt,
            backupCodesCount = backupCodesCount,
        )
    }
}