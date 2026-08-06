package com.usang.stockmarket.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserId(Long userId);
    long countByUserId(Long userId);
    boolean existsByUserIdAndStockSymbol(Long userId, String stockSymbol);
    boolean existsByStockSymbol(String stockSymbol);
    void deleteByUserIdAndStockSymbol(Long userId, String stockSymbol);
    Optional<Watchlist> findByUserIdAndStockSymbol(Long userId, String stockSymbol);
    List<Watchlist> findByStockSymbolAndAlertEnabledTrue(String stockSymbol);
}
