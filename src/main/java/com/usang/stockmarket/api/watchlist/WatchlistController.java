package com.usang.stockmarket.api.watchlist;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.quote.QuoteCache;
import com.usang.stockmarket.application.quote.QuoteUpdate;
import com.usang.stockmarket.application.watchlist.WatchlistService;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
                    String changeRate = quote != null ? quote.changeRate() : null;
                    return new WatchlistItemResponse(stock.getSymbol(), stock.getName(), stock.getMarket(), price, time, changeRate);
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

    @GetMapping("/{stockSymbol}/alert")
    public AlertSettingsResponse getAlertSettings(@PathVariable String stockSymbol, Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        Watchlist watchlist = watchlistService.getWatchlistEntry(userId, stockSymbol);
        return new AlertSettingsResponse(watchlist.isAlertEnabled(), watchlist.getTargetPriceAbove(),
                watchlist.getTargetPriceBelow(), watchlist.getChangeRateThresholdAbove(), watchlist.getChangeRateThresholdBelow());
    }

    @PutMapping("/{stockSymbol}/alert")
    public ResponseEntity<ApiResponse<Void>> updateAlertSettings(@PathVariable String stockSymbol,
            @RequestBody AlertSettingsParam param, Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        watchlistService.updateAlertSettings(userId, stockSymbol, param.alertEnabled(), param.targetPriceAbove(),
                param.targetPriceBelow(), param.changeRateThresholdAbove(), param.changeRateThresholdBelow());
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("알림 설정이 저장되었습니다."));
    }
}

record WatchlistParam(String stockSymbol) {
    public WatchlistParam {
        if(!StringUtils.hasText(stockSymbol)) {
            throw new IllegalArgumentException("종목을 선택해 주세요.");
        }
    }
}

record WatchlistItemResponse(String symbol, String name, String market, String price, String time, String changeRate) {
}

record AlertSettingsResponse(boolean alertEnabled, Integer targetPriceAbove, Integer targetPriceBelow,
                              BigDecimal changeRateThresholdAbove, BigDecimal changeRateThresholdBelow) {
}

record AlertSettingsParam(boolean alertEnabled, Integer targetPriceAbove, Integer targetPriceBelow,
                           BigDecimal changeRateThresholdAbove, BigDecimal changeRateThresholdBelow) {
    public AlertSettingsParam {
        if (targetPriceAbove != null && targetPriceAbove <= 0) {
            throw new IllegalArgumentException("목표가는 0보다 커야 합니다.");
        }
        if (targetPriceBelow != null && targetPriceBelow <= 0) {
            throw new IllegalArgumentException("목표가는 0보다 커야 합니다.");
        }
        if (changeRateThresholdAbove != null && changeRateThresholdAbove.signum() <= 0) {
            throw new IllegalArgumentException("등락률 기준은 0보다 커야 합니다.");
        }
        if (changeRateThresholdBelow != null && changeRateThresholdBelow.signum() <= 0) {
            throw new IllegalArgumentException("등락률 기준은 0보다 커야 합니다.");
        }
    }
}
