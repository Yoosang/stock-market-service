package com.usang.stockmarket.application.watchlist;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import com.usang.stockmarket.infra.kis.KisWebSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {
    private static final int MAX_WATCHLIST_SIZE = 10;

    private final WatchlistRepository watchlistRepository;
    private final StockRepository stockRepository;
    private final KisWebSocketClient kisWebSocketClient;

    public List<Stock> getWatchlistByUserId(Long userId) {
        return watchlistRepository.findByUserId(userId).stream()
                .map(item -> stockRepository.findBySymbol(item.getStockSymbol())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 종목입니다.")))
                .toList();
    }

    public void addWatchlist(Long userId, String stockSymbol) {
        Stock stock = stockRepository.findBySymbol(stockSymbol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 종목입니다."));

        if (!stock.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "상장폐지된 종목입니다.");
        }

        if (watchlistRepository.existsByUserIdAndStockSymbol(userId, stockSymbol)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관심종목에 추가된 종목입니다.");
        }

        if (watchlistRepository.countByUserId(userId) >= MAX_WATCHLIST_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "관심종목은 최대 " + MAX_WATCHLIST_SIZE + "개까지 등록할 수 있습니다.");
        }

        watchlistRepository.save(new Watchlist(userId, stockSymbol));
    }

    @Transactional
    public void removeWatchlist(Long userId, String stockSymbol) {
        if (!watchlistRepository.existsByUserIdAndStockSymbol(userId, stockSymbol)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "관심종목에 없는 종목입니다.");
        }
        watchlistRepository.deleteByUserIdAndStockSymbol(userId, stockSymbol);

        if (!watchlistRepository.existsByStockSymbol(stockSymbol)) {
            kisWebSocketClient.unsubscribe(stockSymbol);
        }
    }
}
