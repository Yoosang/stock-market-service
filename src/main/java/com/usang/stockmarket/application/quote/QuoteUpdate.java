package com.usang.stockmarket.application.quote;

public record QuoteUpdate(String symbol, String price, String time, String changeRate) {
}
