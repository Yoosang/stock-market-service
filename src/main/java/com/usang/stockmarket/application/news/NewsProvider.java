package com.usang.stockmarket.application.news;

import java.util.List;

public interface NewsProvider {

    List<NewsItem> fetchNews(String keyword, int display, String sort);

}