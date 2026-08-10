package com.usang.stockmarket.application.stock;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.infra.kis.KisStockMasterClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSyncServiceTest {

    @Mock
    private KisStockMasterClient kisStockMasterClient;
    @Mock
    private StockRepository stockRepository;

    private StockSyncService stockSyncService;

    @BeforeEach
    void setUp() {
        stockSyncService = new StockSyncService(kisStockMasterClient, stockRepository);
    }

    @Test
    void 마스터파일_종목을_upsert한다() {
        when(kisStockMasterClient.fetchKospi()).thenReturn(List.of(new StockMasterRow("005930", "삼성전자", "KOSPI")));
        when(kisStockMasterClient.fetchKosdaq()).thenReturn(List.of(new StockMasterRow("035720", "카카오", "KOSDAQ")));
        when(stockRepository.findByActiveTrue()).thenReturn(List.of());

        stockSyncService.syncAll();

        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(stockRepository, org.mockito.Mockito.atLeastOnce()).saveAll(captor.capture());
        List<Stock> upserted = captor.getAllValues().get(0);
        assertEquals(2, upserted.size());
        assertTrue(upserted.stream().anyMatch(s -> s.getSymbol().equals("005930")));
        assertTrue(upserted.stream().anyMatch(s -> s.getSymbol().equals("035720")));
    }

    @Test
    void 이전에_활성이었으나_이번에_빠진_종목은_상장폐지처리한다() {
        when(kisStockMasterClient.fetchKospi()).thenReturn(List.of(new StockMasterRow("005930", "삼성전자", "KOSPI")));
        when(kisStockMasterClient.fetchKosdaq()).thenReturn(List.of());
        Stock delistedCandidate = new Stock("999999", "정리매매종목", "KOSPI");
        when(stockRepository.findByActiveTrue()).thenReturn(List.of(delistedCandidate));

        stockSyncService.syncAll();

        assertFalse(delistedCandidate.isActive());
    }

    @Test
    void 마스터파일에_남아있는_종목은_상장폐지처리하지않는다() {
        when(kisStockMasterClient.fetchKospi()).thenReturn(List.of(new StockMasterRow("005930", "삼성전자", "KOSPI")));
        when(kisStockMasterClient.fetchKosdaq()).thenReturn(List.of());
        Stock stillListed = new Stock("005930", "삼성전자", "KOSPI");
        when(stockRepository.findByActiveTrue()).thenReturn(List.of(stillListed));

        stockSyncService.syncAll();

        assertTrue(stillListed.isActive());
    }

    @Test
    void 마스터파일이_비어있으면_기존_활성종목이_모두_상장폐지된다() {
        when(kisStockMasterClient.fetchKospi()).thenReturn(List.of());
        when(kisStockMasterClient.fetchKosdaq()).thenReturn(List.of());
        Stock a = new Stock("005930", "삼성전자", "KOSPI");
        Stock b = new Stock("035720", "카카오", "KOSDAQ");
        when(stockRepository.findByActiveTrue()).thenReturn(List.of(a, b));

        stockSyncService.syncAll();

        assertFalse(a.isActive());
        assertFalse(b.isActive());
    }
}
