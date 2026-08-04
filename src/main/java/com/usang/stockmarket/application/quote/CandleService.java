package com.usang.stockmarket.application.quote;

import com.usang.stockmarket.domain.candle.StockCandle;
import com.usang.stockmarket.domain.candle.StockCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandleService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int BUCKET_MINUTES = 3;
    // 디스크 여유는 충분하지만 무기한 누적을 막는 위생적 상한
    private static final long RETENTION_DAYS = 730;

    private final StockCandleRepository stockCandleRepository;

    @Transactional
    public void recordTick(String symbol, String priceString, String timeString) {
        if (symbol == null || priceString == null || timeString == null || timeString.length() < 4) return;
        int price;
        LocalTime bucketTime;
        try {
            price = Integer.parseInt(priceString);
            bucketTime = toBucket(timeString);
        } catch (RuntimeException e) {
            log.warn("Skipping candle record for {} — unparseable tick", symbol);
            return;
        }
        LocalDate today = LocalDate.now(KST);
        try {
            StockCandle candle = stockCandleRepository
                    .findByStockSymbolAndTradeDateAndBucketTime(symbol, today, bucketTime)
                    .map(existing -> { existing.applyTick(price); return existing; })
                    .orElseGet(() -> new StockCandle(symbol, today, bucketTime, price));
            stockCandleRepository.save(candle);
        } catch (Exception e) {
            // 캔들 저장 실패가 실시간 브로드캐스트를 막아서는 안 된다.
            log.warn("Failed to persist candle tick for {}: {}", symbol, e.getMessage());
        }
    }

    private LocalTime toBucket(String hhmmss) {
        int hour = Integer.parseInt(hhmmss.substring(0, 2));
        int minute = Integer.parseInt(hhmmss.substring(2, 4));
        return LocalTime.of(hour, (minute / BUCKET_MINUTES) * BUCKET_MINUTES);
    }

    public List<StockCandle> getCandles(String symbol, LocalDate date) {
        return stockCandleRepository.findByStockSymbolAndTradeDateOrderByBucketTimeAsc(symbol, date);
    }

    // 매일 새벽 4시(KST) — 보관 기간이 지난 3분봉을 정리한다 (StockSyncService의
    // 월요일 새벽 3시 종목 동기화와 겹치지 않는 시간대).
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void cleanupOldCandles() {
        LocalDate cutoff = LocalDate.now(KST).minusDays(RETENTION_DAYS);
        long deleted = stockCandleRepository.deleteByTradeDateBefore(cutoff);

        log.info("Candle cleanup complete: {} rows deleted (cutoff={})", deleted, cutoff);
    }
}
