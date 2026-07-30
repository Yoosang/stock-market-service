package com.usang.stockmarket.api.stock;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.stock.StockService;
import com.usang.stockmarket.application.stock.StockSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stocks")
public class StockController {

    private final StockService stockService;
    private final StockSyncService stockSyncService;

    @GetMapping
    public List<StockSearchResult> search(@RequestParam String query) {
        return stockService.searchByName(query).stream()
                .map(stock -> new StockSearchResult(stock.getSymbol(), stock.getName(), stock.getMarket()))
                .toList();
    }

    @PostMapping("/sync")
    public ApiResponse<Void> sync() {
        stockSyncService.syncAll();
        return ApiResponse.success("종목 마스터 동기화가 완료되었습니다.");
    }
}

record StockSearchResult(String symbol, String name, String market) {
}
