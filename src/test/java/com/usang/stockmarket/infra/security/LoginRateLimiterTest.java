package com.usang.stockmarket.infra.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    private static final String IP = "127.0.0.1";
    private static final String IP_KEY = "auth:login-fail:ip:" + IP;

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private LoginRateLimiter rateLimiter;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        redisTemplate.delete(IP_KEY);
        rateLimiter = new LoginRateLimiter(redisTemplate, new LoginRateLimitConfiguration(5, 3, 60));
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete(IP_KEY);
    }

    @Test
    void 한도_미만이면_차단하지_않는다() {
        rateLimiter.recordFailure(IP);
        rateLimiter.recordFailure(IP);

        assertDoesNotThrow(() -> rateLimiter.assertNotBlocked(IP));
    }

    @Test
    void IP_실패_횟수가_한도에_도달하면_차단한다() {
        rateLimiter.recordFailure(IP);
        rateLimiter.recordFailure(IP);
        rateLimiter.recordFailure(IP);

        assertThrows(ResponseStatusException.class, () -> rateLimiter.assertNotBlocked(IP));
    }

    @Test
    void 로그인_성공시_카운터가_초기화된다() {
        rateLimiter.recordFailure(IP);
        rateLimiter.recordFailure(IP);

        rateLimiter.recordSuccess(IP);

        assertDoesNotThrow(() -> rateLimiter.assertNotBlocked(IP));
        rateLimiter.recordFailure(IP);
        rateLimiter.recordFailure(IP);
        assertDoesNotThrow(() -> rateLimiter.assertNotBlocked(IP));
    }
}
