package kr.pincoin.api.app.user.common.response

/**
 * TOTP 설정 시작 응답
 *
 * 사용자가 2FA 설정을 시작할 때 필요한 모든 정보를 제공합니다.
 * Google Authenticator 등의 앱에서 사용할 수 있는 QR 코드와 수동 입력 키를 포함합니다.
 */
data class TotpSetupResponse(
    /**
     * QR 코드 생성용 URL
     *
     * otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&digits=6&period=30&algorithm=SHA1
     *
     * 사용자가 Google Authenticator 등의 앱에서 QR 코드를 스캔할 때 사용됩니다.
     * 이 URL을 QR 코드로 변환하여 클라이언트에서 표시해야 합니다.
     */
    val qrCodeUrl: String,

    /**
     * 수동 입력용 키
     *
     * QR 코드 스캔이 불가능한 경우 사용자가 직접 입력할 수 있는 키입니다.
     * 일반적으로 4자리씩 띄어쓰기로 구분된 형태로 제공됩니다.
     * 예: "JBSW Y3DP EHPK 3PXP"
     */
    val manualEntryKey: String,

    /**
     * 백업 코드 목록
     *
     * 디바이스 분실 시 사용할 수 있는 일회용 백업 코드들입니다.
     * 각 코드는 한 번만 사용 가능하며, 사용자가 안전한 곳에 보관해야 합니다.
     * 일반적으로 8-10개의 코드를 제공합니다.
     */
    val backupCodes: List<String>,

    /**
     * 설정 만료 시간 (밀리초)
     *
     * 이 설정 세션이 만료되는 시간입니다.
     * 이 시간 이후에는 새로운 설정을 시작해야 합니다.
     */
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000), // 10분 후

    /**
     * 발급자 이름
     * Google Authenticator 앱에서 표시될 서비스 이름입니다.
     */
    val issuer: String = "PincoinAPI",
) {
    companion object {
        /**
         * 팩토리 메서드 - QR 코드 URL과 수동 입력 키로 응답 생성
         */
        fun create(
            qrCodeUrl: String,
            manualEntryKey: String,
            backupCodes: List<String>,
            issuer: String = "PincoinAPI",
        ) = TotpSetupResponse(
            qrCodeUrl = qrCodeUrl,
            manualEntryKey = manualEntryKey,
            backupCodes = backupCodes,
            issuer = issuer,
        )
    }
}