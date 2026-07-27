package com.usang.stockmarket.infra.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.usang.stockmarket.application.news.NewsItem;
import com.usang.stockmarket.application.news.NewsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Component
@Slf4j
public class NaverNewsProvider implements NewsProvider {

    private final RestClient restClient;
    private final NaverNewsConfiguration naverNewsConfiguration;

    public NaverNewsProvider(
            RestClient.Builder restClientBuilder,
            NaverNewsConfiguration naverNewsConfiguration
            ) {
        this.restClient = restClientBuilder.baseUrl(naverNewsConfiguration.uri()).build();
        this.naverNewsConfiguration = naverNewsConfiguration;
    }

    @Override
    public List<NewsItem> fetchNews(String keyword, int display, String sort) {
        NaverNewsApiResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(naverNewsConfiguration.path())
                            .queryParam("query", keyword)
                            .queryParam("display", display)
                            .queryParam("sort", sort)
                            .build())
                    .header("X-Naver-Client-Id", naverNewsConfiguration.clientId())
                    .header("X-Naver-Client-Secret", naverNewsConfiguration.clientSecret())
                    .retrieve()
                    .body(NaverNewsApiResponse.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch naver new: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "뉴스 조회 중 오류가 발생했습니다.", e);
        }

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> new NewsItem(
                        cleanHtml(item.title()),
                        item.link(),
                        cleanHtml(item.description()),
                        item.pubDate()))
                .toList();
    }

    private String cleanHtml(String text) {
        return HtmlUtils.htmlUnescape(text.replaceAll("<[^>]*>", ""));
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record NaverNewsApiResponse(List<NaverNewsItem> items) {
}

record NaverNewsItem(String title, String originallink, String link, String description, String pubDate) {
}

@ConfigurationProperties(prefix = "app.news.naver")
record NaverNewsConfiguration(String clientId, String clientSecret, String uri, String path) {}