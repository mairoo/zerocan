package kr.pincoin.api.external.auth.keycloak.api.response

/**
 * TOTP 설정용 데이터
 *
 * 사용자가 2FA 설정을 시작할 때 필요한 모든 정보를 포함합니다.
 * Google Authenticator 등의 TOTP 앱에서 사용할 수 있는 형태로 제공됩니다.
 */
data class TotpSetupData(
    /**
     * Base32 인코딩된 TOTP Secret
     * Google Authenticator 앱과 Keycloak에서 동일하게 사용되는 비밀 키
     */
    val secret: String,

    /**
     * QR 코드 생성용 URL
     * otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&digits=6&period=30&algorithm=SHA1
     *
     * 사용자가 Google Authenticator 등의 앱에서 QR 코드를 스캔할 때 사용됩니다.
     */
    val qrCodeUrl: String,

    /**
     * 수동 입력용 키
     * QR 코드 스캔이 불가능한 경우 사용자가 직접 입력할 수 있는 키입니다.
     * 일반적으로 secret과 동일하지만, 사용자 친화적인 형태로 포맷될 수 있습니다.
     */
    val manualEntryKey: String
) {
    companion object {
        /**
         * QR 코드 URL 생성을 위한 팩토리 메서드
         */
        fun create(
            userId: String,
            secret: String,
            issuer: String = "PincoinAPI"
        ): TotpSetupData {
            val qrCodeUrl =
                "otpauth://totp/$issuer:$userId?secret=$secret&issuer=$issuer&digits=6&period=30&algorithm=SHA1"
            val manualEntryKey = formatManualEntryKey(secret)

            return TotpSetupData(
                secret = secret,
                qrCodeUrl = qrCodeUrl,
                manualEntryKey = manualEntryKey
            )
        }

        /**
         * 수동 입력용 키를 사용자 친화적인 형태로 포맷
         * 예: "ABCD1234EFGH5678" -> "ABCD 1234 EFGH 5678"
         */
        private fun formatManualEntryKey(secret: String): String {
            return secret.chunked(4).joinToString(" ")
        }
    }
}