package kr.pincoin.api.external.notification.telegram.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.telegram")
class TelegramProperties {
    var baseUrl: String = "https://api.telegram.org/"
    var botToken: String = ""
    var chatId: String = "-100"
}