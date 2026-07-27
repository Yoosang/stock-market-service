package com.usang.stockmarket.infra.kis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class KisTokenCache {

    private static final String CACHE_KEY = "kis:token";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public Optional<KisTokenCacheData> load() {
        String json = redisTemplate.opsForValue().get(CACHE_KEY);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, KisTokenCacheData.class));
        } catch (IOException e) {
            log.warn("Failed to parse KIS token cache from Redis, ignoring cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void save(KisTokenCacheData data) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(data));
        } catch (IOException e) {
            log.warn("Failed to write KIS token cache to Redis: {}", e.getMessage());
        }
    }
}
