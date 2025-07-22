package kr.pincoin.api.external.notification.slack.service

import com.fasterxml.jackson.databind.ObjectMapper
import kr.pincoin.api.external.notification.slack.api.request.SlackMessagePayload
import kr.pincoin.api.external.notification.slack.api.request.SlackMessageRequest
import kr.pincoin.api.external.notification.slack.api.response.SlackApiResponse
import kr.pincoin.api.external.notification.slack.api.response.SlackMessageResponse
import kr.pincoin.api.external.notification.slack.error.SlackErrorCode
import kr.pincoin.api.external.notification.slack.properties.SlackProperties
import kr.pincoin.api.global.exception.BusinessException
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono


@Component
class SlackMessageApiClient(
    private val slackWebClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val slackProperties: SlackProperties,
) {
    /**
     * 메시지 전송
     * @param messageText 전송할 메시지
     * @param channel 전송할 채널 (옵션, 기본값은 설정된 defaultChannel)
     * @return Mono<SlackApiResponse<SlackMessageResponse>>
     */
    fun sendMessage(
        messageText: SlackMessagePayload,
        channel: String = slackProperties.channel
    ): Mono<SlackApiResponse<SlackMessageResponse>> =
        executeSlackRequest(SlackMessageRequest.of(channel, messageText))

    /**
     * Block Kit 메시지 전송
     * @param blocks Block Kit 형식의 메시지
     * @param channel 전송할 채널 (옵션)
     * @return Mono<SlackApiResponse<SlackMessageResponse>>
     */
    fun sendBlockMessage(
        blocks: List<Map<String, Any>>,
        channel: String = slackProperties.channel
    ): Mono<SlackApiResponse<SlackMessageResponse>> =
        executeSlackRequest(
            request = mapOf(
                "channel" to channel,
                "blocks" to blocks
            )
        )

    /**
     * Slack API 요청 실행 및 응답 처리를 위한 공통 메서드
     * @param request API 요청 본문
     * @return Mono<SlackApiResponse<SlackMessageResponse>>
     */
    private fun executeSlackRequest(request: Any): Mono<SlackApiResponse<SlackMessageResponse>> {
        return slackWebClient
            .post()
            .uri("/chat.postMessage")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String::class.java)
            .flatMap { response: String -> parseSlackResponse(response) }
            .onErrorMap { e ->
                when (e) {
                    is BusinessException -> e
                    else -> BusinessException(SlackErrorCode.SLACK_API_SEND_ERROR)
                }
            }
    }

    /**
     * Slack API 응답 파싱 및 검증
     * @param response API 응답 문자열
     * @return Mono<SlackApiResponse<SlackMessageResponse>>
     */
    private fun parseSlackResponse(response: String): Mono<SlackApiResponse<SlackMessageResponse>> {
        return try {
            val type = objectMapper.typeFactory.constructParametricType(
                SlackApiResponse::class.java,
                SlackMessageResponse::class.java
            )
            val slackApiResponse: SlackApiResponse<SlackMessageResponse> =
                objectMapper.readValue(response, type)

            if (!slackApiResponse.ok) {
                Mono.error(
                    BusinessException(
                        SlackErrorCode.SLACK_API_SEND_ERROR,
                        slackApiResponse.error ?: "Unknown error"
                    )
                )
            } else {
                Mono.just(slackApiResponse)
            }
        } catch (_: Exception) {
            Mono.error(BusinessException(SlackErrorCode.SLACK_API_PARSE_ERROR))
        }
    }
}