package com.usang.stockmarket.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUserId(Long userId);
    boolean existsByUserIdAndStockSymbol(Long userId, String stockSymbol);
    void deleteByUserIdAndStockSymbol(Long userId, String stockSymbol);
}
