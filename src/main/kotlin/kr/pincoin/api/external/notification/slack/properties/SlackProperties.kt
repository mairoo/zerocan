package kr.pincoin.api.external.notification.slack.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.slack")
data class SlackProperties(
    var baseUrl: String = "https://slack.com/api/",
    var botToken: String = "",
    var channel: String = "#general"
)