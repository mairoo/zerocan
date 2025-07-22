package kr.pincoin.api.external.notification.aligo.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.aligo")
class AligoProperties {
    var baseUrl: String = "https://apis.aligo.in"
    var key: String = "7bK9nR4mX2pL8vQ3cY5hJ6tN1wA9dE0s"
    var userId: String = "userId"
    var sender: String = "01012341234"
}