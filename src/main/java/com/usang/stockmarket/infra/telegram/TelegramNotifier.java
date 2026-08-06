package com.usang.stockmarket.infra.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class TelegramNotifier {

    private final RestClient restClient;
    private final String chatId;

    public TelegramNotifier(RestClient.Builder restClientBuilder, TelegramConfiguration telegramConfiguration) {
        this.restClient = restClientBuilder.baseUrl("https://api.telegram.org/bot" + telegramConfiguration.botToken()).build();
        this.chatId = telegramConfiguration.chatId();
    }

    // 이 메서드는 요청 스레드가 아닌 백그라운드 실행기에서 호출되고 기다리는 호출자가 없으므로,
    // 실패해도 예외를 던지지 않고 로그만 남긴다 (호출부인 AlertService.fire 참고).
    public void sendMessage(String text) {
        try {
            TelegramSendMessageResponse response = restClient.post()
                    .uri("/sendMessage")
                    .body(new TelegramSendMessageRequest(chatId, text))
                    .retrieve()
                    .body(TelegramSendMessageResponse.class);
            if (response == null || !response.ok()) {
                log.warn("Telegram sendMessage returned failure: {}", response);
            }
        } catch (RestClientException e) {
            log.error("Failed to send Telegram alert: {}", e.getMessage());
        }
    }
}

record TelegramSendMessageRequest(@JsonProperty("chat_id") String chatId, String text) {
}

@JsonIgnoreProperties(ignoreUnknown = true)
record TelegramSendMessageResponse(boolean ok, String description) {
}
