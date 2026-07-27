package com.usang.stockmarket.application.quote;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteCache {

    private static final String KEY_PREFIX = "quote:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(QuoteUpdate quote) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + quote.symbol(), objectMapper.writeValueAsString(quote));
        } catch (IOException e) {
            log.warn("Failed to write quote cache for {}: {}", quote.symbol(), e.getMessage());
        }
    }

    public Optional<QuoteUpdate> load(String symbol) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + symbol);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, QuoteUpdate.class));
        } catch (IOException e) {
            log.warn("Failed to parse quote cache for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
