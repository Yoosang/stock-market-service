package com.usang.stockmarket.infra.security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationTokenStoreTest {

    private static final String EMAIL = "test@test.com";

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

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

    @AfterEach
    void cleanup() {
        redisTemplate.delete("email-verify:" + EMAIL);
        redisTemplate.delete("email-verified:" + EMAIL);
    }

    @Test
    void 발급한_인증번호를_그대로_조회한다() {
        EmailVerificationTokenStore store = new EmailVerificationTokenStore(redisTemplate);

        String token = store.issue(EMAIL);
        Optional<String> found = store.consume(EMAIL);

        assertTrue(found.isPresent());
        assertEquals(token, found.get());
    }

    @Test
    void 존재하지_않는_이메일은_빈_값을_반환한다() {
        EmailVerificationTokenStore store = new EmailVerificationTokenStore(redisTemplate);

        assertTrue(store.consume("no-such@test.com").isEmpty());
    }

    @Test
    void 무효화하면_더_이상_조회되지_않는다() {
        EmailVerificationTokenStore store = new EmailVerificationTokenStore(redisTemplate);

        store.issue(EMAIL);
        store.invalidate(EMAIL);

        assertTrue(store.consume(EMAIL).isEmpty());
    }

    @Test
    void 인증_완료로_표시하면_한_번만_소비할_수_있다() {
        EmailVerificationTokenStore store = new EmailVerificationTokenStore(redisTemplate);

        store.markVerified(EMAIL);

        assertTrue(store.consumeVerified(EMAIL));
        assertFalse(store.consumeVerified(EMAIL));
    }

    @Test
    void 인증_완료_표시가_없으면_소비에_실패한다() {
        EmailVerificationTokenStore store = new EmailVerificationTokenStore(redisTemplate);

        assertFalse(store.consumeVerified(EMAIL));
    }
}
