package com.usang.stockmarket.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final String IP_KEY_PREFIX = "auth:login-fail:ip:";

    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimitConfiguration rateLimitConfiguration;

    public void assertNotBlocked(String ip) {
        String value = redisTemplate.opsForValue().get(IP_KEY_PREFIX + ip);
        if (value != null && Long.parseLong(value) >= rateLimitConfiguration.maxAttemptsPerIp()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public void recordFailure(String ip) {
        String key = IP_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimitConfiguration.windowSeconds()));
        }
    }

    public void recordSuccess(String ip) {
        redisTemplate.delete(IP_KEY_PREFIX + ip);
    }
}
