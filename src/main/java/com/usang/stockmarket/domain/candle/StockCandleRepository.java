package com.usang.stockmarket.domain.candle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface StockCandleRepository extends JpaRepository<StockCandle, Long> {
    Optional<StockCandle> findByStockSymbolAndTradeDateAndBucketTime(
            String stockSymbol, LocalDate tradeDate, LocalTime bucketTime);

    List<StockCandle> findByStockSymbolAndTradeDateOrderByBucketTimeAsc(
            String stockSymbol, LocalDate tradeDate);

    long deleteByTradeDateBefore(LocalDate cutoff);
}
