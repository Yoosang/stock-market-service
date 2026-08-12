package com.usang.stockmarket.infra.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class TelegramNotifier {

    private final RestClient restClient;
    private final String chatId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TelegramNotifier(RestClient.Builder restClientBuilder, TelegramConfiguration telegramConfiguration) {
        this.restClient = restClientBuilder.baseUrl("https://api.telegram.org/bot" + telegramConfiguration.botToken()).build();
        this.chatId = telegramConfiguration.chatId();
    }

    // 호출 스레드(요청 스레드, KIS 틱 처리 스레드 등)를 텔레그램 API 지연으로 블로킹하지 않기 위해
    // 별도 스레드에서 전송한다. 기다리는 호출자가 없으므로 실패해도 예외를 던지지 않고 로그만 남긴다.
    public void sendAsync(String text) {
        executor.submit(() -> sendMessage(text));
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
