package com.usang.stockmarket.infra.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public record TelegramConfiguration(String botToken, String chatId) {
}
