package com.usang.stockmarket.application.alert;

import com.usang.stockmarket.application.quote.QuoteUpdate;
import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import com.usang.stockmarket.infra.telegram.TelegramNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final TelegramNotifier telegramNotifier;
    private final ExecutorService telegramExecutor = Executors.newSingleThreadExecutor();

    @Transactional
    public void checkAndFire(QuoteUpdate quote) {
        try {
            List<Watchlist> candidates = watchlistRepository.findByStockSymbolAndAlertEnabledTrue(quote.symbol());
            if (candidates.isEmpty()) return;

            BigDecimal price = parse(quote.price());
            if (price == null) return;
            BigDecimal changeRate = parse(quote.changeRate());

            for (Watchlist watchlist : candidates) {
                if (shouldFire(watchlist, price, changeRate)) {
                    fire(watchlist, quote.symbol(), price, changeRate);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Alert check failed for {}: {}", quote.symbol(), e.getMessage());
        }
    }

    private boolean shouldFire(Watchlist watchlist, BigDecimal price, BigDecimal changeRate) {
        boolean priceHit =
                (watchlist.getTargetPriceAbove() != null && price.compareTo(BigDecimal.valueOf(watchlist.getTargetPriceAbove())) >= 0) ||
                (watchlist.getTargetPriceBelow() != null && price.compareTo(BigDecimal.valueOf(watchlist.getTargetPriceBelow())) <= 0);
        // 등락률은 항상 양수 크기로 저장되고(이상=상승 기준, 이하=하락 기준), 이하는 비교 시점에 부호를 반전한다.
        boolean rateHit = changeRate != null && (
                (watchlist.getChangeRateThresholdAbove() != null && changeRate.compareTo(watchlist.getChangeRateThresholdAbove()) >= 0) ||
                (watchlist.getChangeRateThresholdBelow() != null && changeRate.compareTo(watchlist.getChangeRateThresholdBelow().negate()) <= 0));
        return priceHit || rateHit;
    }

    private void fire(Watchlist watchlist, String symbol, BigDecimal price, BigDecimal changeRate) {
        // 텔레그램 전송 전에 먼저 꺼서 다음 틱에서 같은 조건으로 중복 발동하는 것을 막는다.
        // (@Transactional dirty checking으로 커밋 시 자동 반영되므로 명시적 save 불필요)
        watchlist.disableAlert();

        String stockName = stockRepository.findBySymbol(symbol).map(Stock::getName).orElse(symbol);
        String message = buildMessage(stockName, symbol, price, changeRate);

        // KIS 틱 처리 스레드를 텔레그램 API 지연으로 블로킹하지 않기 위해 별도 스레드로 전송한다.
        telegramExecutor.submit(() -> telegramNotifier.sendMessage(message));
    }

    private String buildMessage(String stockName, String symbol, BigDecimal price, BigDecimal changeRate) {
        return "[가격 알림] %s(%s) 현재가 %s원 (변동률 %s%%) 조건을 충족하여 알림을 전송합니다."
                .formatted(stockName, symbol, price.toPlainString(), changeRate == null ? "-" : changeRate.toPlainString());
    }

    private BigDecimal parse(String raw) {
        try {
            return raw == null ? null : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
