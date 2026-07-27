package com.usang.stockmarket.api.watchlist;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.quote.QuoteCache;
import com.usang.stockmarket.application.quote.QuoteUpdate;
import com.usang.stockmarket.application.watchlist.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/watchlist")
public class WatchlistController {
    private final WatchlistService watchlistService;
    private final QuoteCache quoteCache;

    @GetMapping
    public List<WatchlistItemResponse> getAllWatchlists(Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        return watchlistService.getWatchlistByUserId(userId).stream()
                .map(stock -> {
                    QuoteUpdate quote = quoteCache.load(stock.getSymbol()).orElse(null);
                    String price = quote != null ? quote.price() : null;
                    String time = quote != null ? quote.time() : null;
                    return new WatchlistItemResponse(stock.getSymbol(), stock.getName(), stock.getMarket(), price, time);
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addWatchlist(@RequestBody WatchlistParam param, Authentication auth) {
        watchlistService.addWatchlist(Long.parseLong(auth.getPrincipal().toString()), param.stockSymbol());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("종목이 추가 되었습니다."));
    }

    @DeleteMapping("/{stockSymbol}")
    public ResponseEntity<ApiResponse<Void>> deleteWatchlist(@PathVariable String stockSymbol, Authentication auth) {
        watchlistService.removeWatchlist(Long.parseLong(auth.getPrincipal().toString()), stockSymbol);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("종목이 삭제 되었습니다."));
    }
}

record WatchlistParam(String stockSymbol) {
    public WatchlistParam {
        if(!StringUtils.hasText(stockSymbol)) {
            throw new IllegalArgumentException("종목을 선택해 주세요.");
        }
    }
}

record WatchlistItemResponse(String symbol, String name, String market, String price, String time) {
}
