package com.usang.stockmarket.infra.kis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@Slf4j
public class KisTokenCache {

    private static final Path CACHE_FILE = Path.of(".kis-token-cache.json");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public Optional<KisTokenCacheData> load() {
        if (!Files.exists(CACHE_FILE)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(CACHE_FILE.toFile(), KisTokenCacheData.class));
        } catch (IOException e) {
            log.warn("Failed to read KIS token cache file, ignoring cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void save(KisTokenCacheData data) {
        try {
            objectMapper.writeValue(CACHE_FILE.toFile(), data);
        } catch (IOException e) {
            log.warn("Failed to write KIS token cache file: {}", e.getMessage());
        }
    }
}
