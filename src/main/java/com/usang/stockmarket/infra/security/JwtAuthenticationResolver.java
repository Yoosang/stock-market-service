package com.usang.stockmarket.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * "Bearer 토큰 파싱" + "JWT 검증 후 Authentication 생성"을 HTTP 필터와 STOMP 인터셉터가
 * 공통으로 쓰기 위한 헬퍼. 각 진입점(HttpServletRequest, STOMP native header)에서 헤더 값을
 * 꺼내는 방식만 다르고, 그 이후 처리는 동일해서 여기로 모아둔다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationResolver {

    private final JwtTokenProvider jwtTokenProvider;

    public String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    public Authentication resolveAuthentication(String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return null;
        }
        Long userId = Long.valueOf(jwtTokenProvider.getUserId(token));
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
