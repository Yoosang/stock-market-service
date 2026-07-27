package com.usang.stockmarket.infra.kis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KisTokenCacheTest {

    private static final String CACHE_KEY = "kis:token";

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

    @BeforeEach
    void setUp() {
        // 실제 앱 실행이나 다른 테스트가 같은 키에 캐시를 남겨둘 수 있어 시작 전에도 정리한다.
        redisTemplate.delete(CACHE_KEY);
    }

    @AfterEach
    void cleanup() {
        redisTemplate.delete(CACHE_KEY);
    }

    @Test
    void 저장한_토큰을_그대로_불러온다() {
        KisTokenCache cache = new KisTokenCache(redisTemplate);

        Instant accessExpiresAt = Instant.now().plusSeconds(3600);
        Instant approvalExpiresAt = Instant.now().plusSeconds(86400);
        KisTokenCacheData saved = new KisTokenCacheData(
                "test-access-token", accessExpiresAt,
                "test-approval-key", approvalExpiresAt);

        cache.save(saved);

        assertTrue(redisTemplate.hasKey(CACHE_KEY), "캐시가 Redis에 저장되어야 한다.");

        Optional<KisTokenCacheData> loaded = cache.load();
        assertTrue(loaded.isPresent(), "저장한 캐시를 다시 읽을 수 있어야 한다.");
        assertEquals(saved, loaded.get());
    }

    @Test
    void 캐시_키가_없으면_빈_값을_반환한다() {
        KisTokenCache cache = new KisTokenCache(redisTemplate);

        assertTrue(cache.load().isEmpty());
    }
}
