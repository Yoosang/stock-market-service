package com.usang.stockmarket.domain.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name="stocks")
public class Stock {
    @Id
    private String symbol;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String market;
}
