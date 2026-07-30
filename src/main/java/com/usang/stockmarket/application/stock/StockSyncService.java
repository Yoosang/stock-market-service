package com.usang.stockmarket.application.stock;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.infra.kis.KisStockMasterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockSyncService {

    private final KisStockMasterClient kisStockMasterClient;
    private final StockRepository stockRepository;

    // 매주 월요일 새벽 3시(KST) — 신규상장/상장폐지는 매일 확인할 만큼 잦은 이벤트가 아니라 주 1회로 충분하다.
    @Scheduled(cron = "0 0 3 * * MON", zone = "Asia/Seoul")
    public void scheduledSync() {
        syncAll();
    }

    /**
     * KIS 종목 마스터파일(코스피+코스닥)을 받아 stocks 테이블과 동기화한다.
     * 마스터파일에 있는 종목은 upsert(active=true), 이전엔 active였지만 이번엔 빠진 종목은
     * 상장폐지로 간주해 active=false로 전환한다. 재실행해도 안전하다(idempotent).
     */
    public void syncAll() {
        List<StockMasterRow> rows = Stream.concat(
                kisStockMasterClient.fetchKospi().stream(),
                kisStockMasterClient.fetchKosdaq().stream()
        ).toList();

        List<Stock> stocks = rows.stream()
                .map(row -> new Stock(row.symbol(), row.name(), row.market()))
                .toList();
        stockRepository.saveAll(stocks);

        Set<String> fetchedSymbols = new HashSet<>();
        rows.forEach(row -> fetchedSymbols.add(row.symbol()));

        List<Stock> newlyDelisted = stockRepository.findByActiveTrue().stream()
                .filter(stock -> !fetchedSymbols.contains(stock.getSymbol()))
                .toList();
        newlyDelisted.forEach(Stock::markInactive);
        stockRepository.saveAll(newlyDelisted);

        log.info("Stock sync complete: {} fetched, {} newly marked delisted", rows.size(), newlyDelisted.size());
    }
}
