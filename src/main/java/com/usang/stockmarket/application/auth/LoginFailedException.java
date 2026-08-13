package com.usang.stockmarket.application.auth;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class LoginFailedException extends ResponseStatusException {

    private final int remainingAttempts;

    public LoginFailedException(HttpStatusCode status, String reason, int remainingAttempts) {
        super(status, reason);
        this.remainingAttempts = remainingAttempts;
    }
}
