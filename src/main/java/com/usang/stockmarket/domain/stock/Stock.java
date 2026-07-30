package com.usang.stockmarket.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name="stocks")
public class Stock {
    @Id
    private String symbol;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String market;
    @Column(nullable = false)
    private boolean active = true;

    public Stock(String symbol, String name, String market) {
        this.symbol = symbol;
        this.name = name;
        this.market = market;
        this.active = true;
    }

    public void markInactive() {
        this.active = false;
    }
}
