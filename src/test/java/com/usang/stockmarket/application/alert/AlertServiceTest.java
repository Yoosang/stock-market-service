package com.usang.stockmarket.application.alert;

import com.usang.stockmarket.application.quote.QuoteUpdate;
import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import com.usang.stockmarket.infra.telegram.TelegramNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private TelegramNotifier telegramNotifier;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(watchlistRepository, stockRepository, telegramNotifier);
    }

    private Watchlist watchlistWithThresholds(Integer priceAbove, Integer priceBelow,
            BigDecimal rateAbove, BigDecimal rateBelow) {
        Watchlist watchlist = new Watchlist(1L, "005930");
        watchlist.updateAlertSettings(true, priceAbove, priceBelow, rateAbove, rateBelow);
        return watchlist;
    }

    @Test
    void 후보가_없으면_아무것도_하지않는다() {
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of());

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "1.5"));

        verifyNoInteractions(stockRepository, telegramNotifier);
    }

    @Test
    void 가격이_숫자로_파싱되지않으면_무시한다() {
        Watchlist watchlist = watchlistWithThresholds(60000, null, null, null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));

        alertService.checkAndFire(new QuoteUpdate("005930", "N/A", "090300", "1.5"));

        verifyNoInteractions(stockRepository, telegramNotifier);
    }

    @Test
    void 상한가_목표가_도달시_알림을_보내고_비활성화한다() {
        Watchlist watchlist = watchlistWithThresholds(70000, null, null, null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "1.5"));

        assertFalse(watchlist.isAlertEnabled());
        verify(telegramNotifier, timeout(500)).sendAsync(anyString());
    }

    @Test
    void 하한가_목표가_도달시_알림을_보내고_비활성화한다() {
        Watchlist watchlist = watchlistWithThresholds(null, 60000, null, null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "59000", "090300", "-1.5"));

        assertFalse(watchlist.isAlertEnabled());
        verify(telegramNotifier, timeout(500)).sendAsync(anyString());
    }

    @Test
    void 상승률_임계값_도달시_알림을_보낸다() {
        Watchlist watchlist = watchlistWithThresholds(null, null, BigDecimal.valueOf(5), null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "5.5"));

        assertFalse(watchlist.isAlertEnabled());
        verify(telegramNotifier, timeout(500)).sendAsync(anyString());
    }

    @Test
    void 하락률_임계값_도달시_알림을_보낸다() {
        // changeRateThresholdBelow는 항상 양수 크기로 저장하고, 비교 시점에 부호를 반전한다.
        Watchlist watchlist = watchlistWithThresholds(null, null, null, BigDecimal.valueOf(5));
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "65000", "090300", "-5.5"));

        assertFalse(watchlist.isAlertEnabled());
        verify(telegramNotifier, timeout(500)).sendAsync(anyString());
    }

    @Test
    void 조건을_충족하지않으면_알림을_보내지않는다() {
        Watchlist watchlist = watchlistWithThresholds(80000, 60000, BigDecimal.valueOf(5), BigDecimal.valueOf(5));
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "1.0"));

        assertTrue(watchlist.isAlertEnabled());
        verifyNoInteractions(stockRepository, telegramNotifier);
    }

    @Test
    void 등락률이_null이어도_가격조건은_평가된다() {
        Watchlist watchlist = watchlistWithThresholds(70000, null, null, null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(watchlist));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", null));

        assertFalse(watchlist.isAlertEnabled());
        verify(telegramNotifier, timeout(500)).sendAsync(anyString());
    }

    @Test
    void 여러_후보중_조건을_충족한_것만_알림을_보낸다() {
        Watchlist hit = watchlistWithThresholds(70000, null, null, null);
        Watchlist miss = watchlistWithThresholds(90000, null, null, null);
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930")).thenReturn(List.of(hit, miss));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(new Stock("005930", "삼성전자", "KOSPI")));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "1.0"));

        assertFalse(hit.isAlertEnabled());
        assertTrue(miss.isAlertEnabled());
        verify(telegramNotifier, timeout(500).times(1)).sendAsync(anyString());
    }

    @Test
    void 저장소_예외가_발생해도_예외를_전파하지않는다() {
        when(watchlistRepository.findByStockSymbolAndAlertEnabledTrue("005930"))
                .thenThrow(new RuntimeException("db down"));

        alertService.checkAndFire(new QuoteUpdate("005930", "70000", "090300", "1.0"));

        verifyNoInteractions(telegramNotifier);
    }
}
