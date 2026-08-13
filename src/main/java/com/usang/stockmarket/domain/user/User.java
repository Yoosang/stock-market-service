package com.usang.stockmarket.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="users")
@NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Integer failedLoginCount;

    private LocalDateTime lockedUntil;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.failedLoginCount = 0;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void incrementFailedLoginCount() {
        this.failedLoginCount++;
    }

    public void lockUntil(LocalDateTime until) {
        this.lockedUntil = until;
    }

    public void resetLoginFailure() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }
}