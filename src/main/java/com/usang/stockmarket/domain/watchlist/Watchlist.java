package com.usang.stockmarket.domain.watchlist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public Watchlist(Long userId, String stockSymbol) {
        this.userId = userId;
        this.stockSymbol = stockSymbol;
        this.createdAt = LocalDateTime.now();
    }
}
