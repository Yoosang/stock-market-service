package com.usang.stockmarket.infra.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtAuthenticationResolver jwtAuthenticationResolver;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = jwtAuthenticationResolver.extractBearerToken(accessor.getFirstNativeHeader("Authorization"));
            Authentication authentication = jwtAuthenticationResolver.resolveAuthentication(token);

            if (authentication == null) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
            }

            accessor.setUser(authentication);
        }

        return message;
    }
}
