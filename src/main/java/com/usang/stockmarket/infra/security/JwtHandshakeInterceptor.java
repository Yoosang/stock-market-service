package com.usang.stockmarket.infra.security;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * STOMP는 CONNECT 프레임에 쿠키가 자동으로 실리지 않으므로(브라우저가 쿠키를 자동 첨부하는 건
 * 실제 HTTP 요청뿐), 그 앞 단계인 WebSocket 핸드셰이크(HTTP 업그레이드 요청)에서 인증을 확인한다.
 * 여기서 검증한 Authentication은 attributes에 담아두고, JwtHandshakeUserHandler가 STOMP 세션의
 * Principal로 연결한다.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTH_ATTRIBUTE = "authentication";

    private final JwtAuthenticationResolver jwtAuthenticationResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Cookie[] cookies = servletRequest.getServletRequest().getCookies();
        String token = jwtAuthenticationResolver.extractTokenFromCookies(cookies);
        Authentication authentication = jwtAuthenticationResolver.resolveAuthentication(token);

        if (authentication == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(AUTH_ATTRIBUTE, authentication);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
