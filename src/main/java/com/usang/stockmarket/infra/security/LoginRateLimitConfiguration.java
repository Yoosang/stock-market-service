package com.usang.stockmarket.infra.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.login-rate-limit")
public record LoginRateLimitConfiguration(int maxAttemptsPerEmail, int maxAttemptsPerIp, long windowSeconds) {
}
