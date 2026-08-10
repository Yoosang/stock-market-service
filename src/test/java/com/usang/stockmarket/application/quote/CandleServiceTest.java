package com.usang.stockmarket.application.quote;

import com.usang.stockmarket.domain.candle.StockCandle;
import com.usang.stockmarket.domain.candle.StockCandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandleServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private StockCandleRepository stockCandleRepository;

    private CandleService candleService;

    @BeforeEach
    void setUp() {
        candleService = new CandleService(stockCandleRepository);
    }

    @Test
    void 기존_버킷이_없으면_새_캔들을_생성한다() {
        when(stockCandleRepository.findByStockSymbolAndTradeDateAndBucketTime(
                eq("005930"), eq(LocalDate.now(KST)), eq(LocalTime.of(9, 0)))).thenReturn(Optional.empty());

        candleService.recordTick("005930", "70000", "090059");

        ArgumentCaptor<StockCandle> captor = ArgumentCaptor.forClass(StockCandle.class);
        verify(stockCandleRepository).save(captor.capture());
        StockCandle saved = captor.getValue();
        assertEquals(70000, saved.getOpenPrice());
        assertEquals(70000, saved.getHighPrice());
        assertEquals(70000, saved.getLowPrice());
        assertEquals(70000, saved.getClosePrice());
    }

    @Test
    void 같은_버킷에_틱이_오면_고가_저가_종가를_갱신한다() {
        StockCandle existing = new StockCandle("005930", LocalDate.now(KST), LocalTime.of(9, 0), 70000);
        when(stockCandleRepository.findByStockSymbolAndTradeDateAndBucketTime(
                eq("005930"), eq(LocalDate.now(KST)), eq(LocalTime.of(9, 0)))).thenReturn(Optional.of(existing));

        candleService.recordTick("005930", "71000", "090100");

        ArgumentCaptor<StockCandle> captor = ArgumentCaptor.forClass(StockCandle.class);
        verify(stockCandleRepository).save(captor.capture());
        StockCandle saved = captor.getValue();
        assertEquals(70000, saved.getOpenPrice());
        assertEquals(71000, saved.getHighPrice());
        assertEquals(70000, saved.getLowPrice());
        assertEquals(71000, saved.getClosePrice());
    }

    @ParameterizedTest
    @CsvSource({
            "090259, 09:00",
            "090300, 09:03",
            "090559, 09:03",
            "090600, 09:06"
    })
    void 버킷_경계값_검증(String tickTime, String expectedBucket) {
        when(stockCandleRepository.findByStockSymbolAndTradeDateAndBucketTime(any(), any(), any()))
                .thenReturn(Optional.empty());

        candleService.recordTick("005930", "70000", tickTime);

        verify(stockCandleRepository).findByStockSymbolAndTradeDateAndBucketTime(
                eq("005930"), eq(LocalDate.now(KST)), eq(LocalTime.parse(expectedBucket)));
    }

    @Test
    void symbol_price_time_중_하나라도_null이면_무시한다() {
        candleService.recordTick(null, "70000", "090300");
        candleService.recordTick("005930", null, "090300");
        candleService.recordTick("005930", "70000", null);

        verifyNoInteractions(stockCandleRepository);
    }

    @Test
    void 시간문자열_길이가_4미만이면_무시한다() {
        candleService.recordTick("005930", "70000", "090");

        verifyNoInteractions(stockCandleRepository);
    }

    @Test
    void 가격이_숫자가_아니면_무시한다() {
        candleService.recordTick("005930", "abc", "090300");

        verifyNoInteractions(stockCandleRepository);
    }

    @Test
    void 저장중_예외가_발생해도_예외를_전파하지않는다() {
        when(stockCandleRepository.findByStockSymbolAndTradeDateAndBucketTime(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(stockCandleRepository.save(any())).thenThrow(new RuntimeException("db down"));

        candleService.recordTick("005930", "70000", "090300");
    }

    @Test
    void retention_기준일_이전_데이터를_정리한다() {
        LocalDate expectedCutoff = LocalDate.now(KST).minusDays(730);

        candleService.cleanupOldCandles();

        verify(stockCandleRepository).deleteByTradeDateBefore(expectedCutoff);
    }
}
