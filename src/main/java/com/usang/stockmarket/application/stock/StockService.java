package com.usang.stockmarket.application.stock;

import com.usang.stockmarket.domain.stock.Stock;
import com.usang.stockmarket.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public List<Stock> searchByName(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return stockRepository.findTop10ByNameContainingIgnoreCaseAndActiveTrue(query);
    }
}
