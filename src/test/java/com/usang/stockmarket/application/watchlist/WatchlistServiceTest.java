package com.usang.stockmarket.application.watchlist;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import com.usang.stockmarket.infra.kis.KisWebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private KisWebSocketClient kisWebSocketClient;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistRepository, stockRepository, kisWebSocketClient);
    }

    @Test
    void 관심종목_목록을_종목정보와_함께_반환한다() {
        Watchlist entry = new Watchlist(1L, "005930");
        Stock stock = new Stock("005930", "삼성전자", "KOSPI");
        when(watchlistRepository.findByUserId(1L)).thenReturn(List.of(entry));
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(stock));

        List<Stock> result = watchlistService.getWatchlistByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("005930", result.get(0).getSymbol());
    }

    @Test
    void 관심종목에_있는_종목이_삭제되어_없으면_예외를_던진다() {
        Watchlist entry = new Watchlist(1L, "999999");
        when(watchlistRepository.findByUserId(1L)).thenReturn(List.of(entry));
        when(stockRepository.findBySymbol("999999")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.getWatchlistByUserId(1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void 존재하지않는_종목을_추가하면_예외를_던진다() {
        when(stockRepository.findBySymbol("999999")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.addWatchlist(1L, "999999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void 상장폐지된_종목을_추가하면_예외를_던진다() {
        Stock delisted = new Stock("005930", "삼성전자", "KOSPI");
        delisted.markInactive();
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(delisted));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.addWatchlist(1L, "005930"));

        assertEquals(HttpStatus.GONE, exception.getStatusCode());
    }

    @Test
    void 이미_추가된_종목을_다시_추가하면_예외를_던진다() {
        Stock stock = new Stock("005930", "삼성전자", "KOSPI");
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.addWatchlist(1L, "005930"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void 최대_10개를_초과하면_예외를_던진다() {
        Stock stock = new Stock("005930", "삼성전자", "KOSPI");
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(false);
        when(watchlistRepository.countByUserId(1L)).thenReturn(10L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.addWatchlist(1L, "005930"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(watchlistRepository, never()).save(any());
    }

    @Test
    void 정상_추가시_저장된다() {
        Stock stock = new Stock("005930", "삼성전자", "KOSPI");
        when(stockRepository.findBySymbol("005930")).thenReturn(Optional.of(stock));
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(false);
        when(watchlistRepository.countByUserId(1L)).thenReturn(0L);

        watchlistService.addWatchlist(1L, "005930");

        verify(watchlistRepository).save(any(Watchlist.class));
    }

    @Test
    void 목록에_없는_종목을_삭제하면_예외를_던진다() {
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.removeWatchlist(1L, "005930"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void 마지막_구독자가_삭제하면_KIS_구독을_해제한다() {
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(true);
        when(watchlistRepository.existsByStockSymbol("005930")).thenReturn(false);

        watchlistService.removeWatchlist(1L, "005930");

        verify(kisWebSocketClient).unsubscribe("005930");
    }

    @Test
    void 다른_사용자가_아직_구독중이면_구독을_해제하지_않는다() {
        when(watchlistRepository.existsByUserIdAndStockSymbol(1L, "005930")).thenReturn(true);
        when(watchlistRepository.existsByStockSymbol("005930")).thenReturn(true);

        watchlistService.removeWatchlist(1L, "005930");

        verify(kisWebSocketClient, never()).unsubscribe(any());
    }

    @Test
    void 존재하지않는_관심종목의_알림설정을_변경하면_예외를_던진다() {
        when(watchlistRepository.findByUserIdAndStockSymbol(1L, "005930")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> watchlistService.updateAlertSettings(1L, "005930", true, 90000, 80000,
                        BigDecimal.valueOf(5), BigDecimal.valueOf(-5)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void 알림설정_변경시_값이_반영된다() {
        Watchlist entry = new Watchlist(1L, "005930");
        when(watchlistRepository.findByUserIdAndStockSymbol(1L, "005930")).thenReturn(Optional.of(entry));

        watchlistService.updateAlertSettings(1L, "005930", true, 90000, 80000,
                BigDecimal.valueOf(5), BigDecimal.valueOf(-5));

        assertTrue(entry.isAlertEnabled());
        assertEquals(90000, entry.getTargetPriceAbove());
        assertEquals(80000, entry.getTargetPriceBelow());
        assertEquals(BigDecimal.valueOf(5), entry.getChangeRateThresholdAbove());
        assertEquals(BigDecimal.valueOf(-5), entry.getChangeRateThresholdBelow());
    }
}
