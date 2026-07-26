package com.usang.stockmarket.infra.naver;

import com.usang.stockmarket.application.news.NewsItem;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실제 네이버 뉴스 검색 API를 호출하는 통합 테스트.
 * Spring 컨텍스트 없이 NaverNewsProvider를 직접 생성해서 호출 결과를 눈으로 확인하는 용도.
 * 프로젝트 루트의 key.env에서 NAVER_CLIENT_ID/NAVER_CLIENT_SECRET을 직접 읽으며,
 * 파일이 없거나 키가 없으면 스킵된다 (Run Configuration의 env 설정에 의존하지 않음).
 */
class NaverNewsProviderIntegrationTest {

    @Test
    void 네이버_API_원본_응답을_그대로_출력한다() throws IOException {
        Map<String, String> env = readKeyEnv();
        String clientId = env.get("NAVER_CLIENT_ID");
        String clientSecret = env.get("NAVER_CLIENT_SECRET");
        assumeTrue(clientId != null && clientSecret != null,
                "key.env에 NAVER_CLIENT_ID/NAVER_CLIENT_SECRET이 없어 테스트를 건너뜁니다.");

        RestClient rawClient = RestClient.builder()
                .baseUrl("https://openapi.naver.com")
                .build();

        String rawJson = rawClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/search/news.json")
                        .queryParam("query", "삼성전자")
                        .queryParam("display", 5)
                        .queryParam("sort", "date")
                        .build())
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(String.class);

        System.out.println(rawJson);
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
