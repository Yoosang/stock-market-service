package com.usang.stockmarket.infra.kis;

import com.usang.stockmarket.application.quote.QuoteCache;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 실제로 구현한 KisAuthClient/KisWebSocketClient를 그대로 사용해서
 * KIS에 연결하고 실시간 체결가가 콘솔에 찍히는지 눈으로 확인하는 테스트.
 * WatchlistRepository만 Mockito로 대체해서 DB 없이 실행 가능하다.
 * key.env에 KIS_APP_KEY/KIS_APP_SECRET이 없으면 스킵된다.
 * 장 시간이 아니면 체결 데이터가 안 올 수 있다 (그 경우 SUBSCRIBE SUCCESS 로그까지만 확인).
 */
class KisWebSocketConnectivityTest {

    @Test
    void KisWebSocketClient로_실제_KIS에_연결해서_구독한다() throws Exception {
        Map<String, String> env = readKeyEnv();
        String appKey = env.get("KIS_APP_KEY");
        String appSecret = env.get("KIS_APP_SECRET");
        assumeTrue(appKey != null && appSecret != null,
                "key.env에 KIS_APP_KEY/KIS_APP_SECRET이 없어 테스트를 건너뜁니다.");

        KisConfiguration config = new KisConfiguration(
                appKey, appSecret,
                "https://openapi.koreainvestment.com:9443", "/oauth2/Approval",
                "ws://ops.koreainvestment.com:21000");

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        KisAuthClient authClient = new KisAuthClient(config, RestClient.builder(), new KisTokenCache(redisTemplate));

        WatchlistRepository fakeWatchlistRepository = mock(WatchlistRepository.class);
        when(fakeWatchlistRepository.findAll())
                .thenReturn(List.of(new Watchlist(1L, "005930")));

        SimpMessagingTemplate fakeMessagingTemplate = mock(SimpMessagingTemplate.class);
        QuoteCache quoteCache = new QuoteCache(redisTemplate);

        KisWebSocketClient client = new KisWebSocketClient(
                authClient, config, new ObjectMapper(), fakeWatchlistRepository, fakeMessagingTemplate, quoteCache);
        client.connect();

        // client.connect()는 비동기로 연결되므로, 콘솔에 SUBSCRIBE SUCCESS/체결가 로그가
        // 찍히는 걸 눈으로 확인할 시간을 준다.
        TimeUnit.SECONDS.sleep(15);

        // 실제 체결 데이터가 왔다면 /topic/quotes/005930으로 broadcast 시도했어야 한다.
        verify(fakeMessagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));

        // 최신 체결가가 Redis에도 캐싱되어 있어야 한다.
        assertTrue(quoteCache.load("005930").isPresent(), "최신 시세가 Redis에 캐싱되어 있어야 한다.");
    }

    private Map<String, String> readKeyEnv() throws IOException {
        Path path = Path.of("key.env");
        Map<String, String> env = new HashMap<>();
        if (!Files.exists(path)) {
            return env;
        }
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int idx = line.indexOf('=');
            env.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
        }
        return env;
    }
}
