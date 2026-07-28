package com.usang.stockmarket.application.news;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsProvider newsProvider;
    private final StockRepository stockRepository;

    public NewsResult getNewsBySymbol(String symbol, int display, String sort) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 종목입니다."));

        List<NewsItem> items = newsProvider.fetchNews(stock.getName(), display, sort);
        return new NewsResult(stock.getSymbol(), stock.getName(), items);
    }
}
