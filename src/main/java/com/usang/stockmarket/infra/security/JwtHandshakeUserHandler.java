package com.usang.stockmarket.infra.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * JwtHandshakeInterceptor가 handshake attributes에 담아둔 Authentication을 STOMP 세션의
 * Principal로 연결한다. 이렇게 하면 STOMP CONNECT 프레임 단계에서 별도로 인증을 다시 확인할
 * 필요 없이, 이후 모든 프레임에서 accessor.getUser()로 그대로 사용할 수 있다.
 */
@Component
public class JwtHandshakeUserHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        return (Principal) attributes.get(JwtHandshakeInterceptor.AUTH_ATTRIBUTE);
    }
}
