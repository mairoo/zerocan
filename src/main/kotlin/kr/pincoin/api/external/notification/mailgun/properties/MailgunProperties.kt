package kr.pincoin.api.external.notification.mailgun.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.mailgun")
class MailgunProperties {
    var baseUrl: String = "https://api.mailgun.net"
    var key: String = "7bK9nR4mX2pL8vQ3cY5hJ6tN1wA9dE0s"
    var domain: String = "mg1.example.com"
    var from: String = "고객센터 <help@example.com>"
}