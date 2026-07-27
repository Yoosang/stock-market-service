package com.usang.stockmarket.infra.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kis")
public record KisConfiguration(String appKey, String appSecret, String restUri, String restPath, String wsUri) {
}
