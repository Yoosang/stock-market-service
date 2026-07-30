package com.usang.stockmarket.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findBySymbol(String symbol);
    List<Stock> findTop10ByNameContainingIgnoreCaseAndActiveTrue(String keyword);
    List<Stock> findByActiveTrue();
}
