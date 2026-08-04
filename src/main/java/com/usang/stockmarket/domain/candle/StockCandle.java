package com.usang.stockmarket.domain.candle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "stock_candles")
public class StockCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stockSymbol;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private LocalTime bucketTime;

    @Column(nullable = false)
    private Integer openPrice;

    @Column(nullable = false)
    private Integer highPrice;

    @Column(nullable = false)
    private Integer lowPrice;

    @Column(nullable = false)
    private Integer closePrice;

    public StockCandle(String stockSymbol, LocalDate tradeDate, LocalTime bucketTime, int price) {
        this.stockSymbol = stockSymbol;
        this.tradeDate = tradeDate;
        this.bucketTime = bucketTime;
        this.openPrice = price;
        this.highPrice = price;
        this.lowPrice = price;
        this.closePrice = price;
    }

    public void applyTick(int price) {
        if (price > highPrice) highPrice = price;
        if (price < lowPrice) lowPrice = price;
        closePrice = price;
    }
}
