package com.usang.stockmarket.api.news;

import com.usang.stockmarket.application.news.NewsResult;
import com.usang.stockmarket.application.news.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks/{symbol}/news")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public NewsResult getNews(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "date") String sort) {
        return newsService.getNewsBySymbol(symbol, display, sort);
    }
}
