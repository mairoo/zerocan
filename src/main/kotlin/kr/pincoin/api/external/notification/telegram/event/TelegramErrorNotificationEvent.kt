package kr.pincoin.api.external.notification.telegram.event

import java.time.LocalDateTime

data class TelegramErrorNotificationEvent(
    val errorMessage: String,
    val errorCode: String? = null,
    val errorDetails: String? = null,
    val serviceName: String,
    val errorSource: String,
    val occurred: LocalDateTime = LocalDateTime.now(),
    val transactionId: String? = null
)