package com.usang.stockmarket.domain.watchlist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name="watchlist")
public class Watchlist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String stockSymbol;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean alertEnabled = false;

    private Integer targetPriceAbove;

    private Integer targetPriceBelow;

    @Column(precision = 5, scale = 2)
    private BigDecimal changeRateThresholdAbove;

    @Column(precision = 5, scale = 2)
    private BigDecimal changeRateThresholdBelow;

    public Watchlist(Long userId, String stockSymbol) {
        this.userId = userId;
        this.stockSymbol = stockSymbol;
        this.createdAt = LocalDateTime.now();
    }

    public void updateAlertSettings(boolean alertEnabled, Integer targetPriceAbove, Integer targetPriceBelow,
                                     BigDecimal changeRateThresholdAbove, BigDecimal changeRateThresholdBelow) {
        this.alertEnabled = alertEnabled;
        this.targetPriceAbove = targetPriceAbove;
        this.targetPriceBelow = targetPriceBelow;
        this.changeRateThresholdAbove = changeRateThresholdAbove;
        this.changeRateThresholdBelow = changeRateThresholdBelow;
    }

    public void disableAlert() {
        this.alertEnabled = false;
    }
}
