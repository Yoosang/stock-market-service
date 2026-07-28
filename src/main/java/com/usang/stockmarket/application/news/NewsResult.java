package com.usang.stockmarket.application.news;

import java.util.List;

public record NewsResult(String symbol, String name, List<NewsItem> items) {
}
