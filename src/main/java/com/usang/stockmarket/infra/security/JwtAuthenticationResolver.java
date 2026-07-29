package com.usang.stockmarket.infra.security;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * JWT를 httpOnly 쿠키로 주고받기 위한 공통 헬퍼. 쿠키 추출/발급과 "JWT 검증 후 Authentication
 * 생성"을 HTTP 필터, STOMP 핸드셰이크 인터셉터, 컨트롤러가 공통으로 쓰기 위해 모아둔다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationResolver {

    public static final String COOKIE_NAME = "ACCESS_TOKEN";

    private final JwtTokenProvider jwtTokenProvider;

    public String extractTokenFromCookies(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Authentication resolveAuthentication(String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return null;
        }
        Long userId = Long.valueOf(jwtTokenProvider.getUserId(token));
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false) // 로컬 개발(http)용. 배포 시 HTTPS 적용하면 true로 전환
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getExpirationMs()))
                .build();
    }

    public ResponseCookie buildLogoutCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
