package com.usang.stockmarket.infra.security;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.jwt.cookie-secure}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie-same-site}")
    private String cookieSameSite;

    // app.finflow.pe.kr(Vercel)의 서버 컴포넌트가 next/headers cookies()로 이 쿠키를 읽으려면,
    // 브라우저가 app.finflow.pe.kr로 가는 요청에도 쿠키를 실어야 한다. Domain을 지정하지 않으면
    // 쿠키를 발급한 호스트(api.finflow.pe.kr)에만 실리는 host-only 쿠키가 되어 app 서브도메인에는
    // 절대 전달되지 않는다. 로컬 개발(둘 다 localhost, 포트만 다름)에서는 이 속성이 없어도 우연히
    // 공유되어 문제가 드러나지 않았다.
    @Value("${app.jwt.cookie-domain:}")
    private String cookieDomain;

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
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .domain(cookieDomain.isBlank() ? null : cookieDomain)
                .path("/")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getExpirationMs()))
                .build();
    }

    public ResponseCookie buildLogoutCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .domain(cookieDomain.isBlank() ? null : cookieDomain)
                .path("/")
                .maxAge(0)
                .build();
    }
}
