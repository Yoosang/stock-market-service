package com.usang.stockmarket.infra.kis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KisTokenCacheTest {

    private static final Path CACHE_FILE = Path.of(".kis-token-cache.json");

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(CACHE_FILE);
    }

    @Test
    void 저장한_토큰을_그대로_불러온다() {
        KisTokenCache cache = new KisTokenCache();

        Instant accessExpiresAt = Instant.now().plusSeconds(3600);
        Instant approvalExpiresAt = Instant.now().plusSeconds(86400);
        KisTokenCacheData saved = new KisTokenCacheData(
                "test-access-token", accessExpiresAt,
                "test-approval-key", approvalExpiresAt);

        cache.save(saved);

        assertTrue(Files.exists(CACHE_FILE), "캐시 파일이 생성되어야 한다.");

        Optional<KisTokenCacheData> loaded = cache.load();
        assertTrue(loaded.isPresent(), "저장한 캐시를 다시 읽을 수 있어야 한다.");
        assertEquals(saved, loaded.get());
    }

    @Test
    void 캐시_파일이_없으면_빈_값을_반환한다() {
        KisTokenCache cache = new KisTokenCache();

        assertTrue(cache.load().isEmpty());
    }
}
