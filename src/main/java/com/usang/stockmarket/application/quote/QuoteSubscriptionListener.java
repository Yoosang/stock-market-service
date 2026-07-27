package com.usang.stockmarket.application.quote;

import com.usang.stockmarket.infra.kis.KisWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteSubscriptionListener {

    private static final String DESTINATION_PREFIX = "/topic/quotes/";

    private final KisWebSocketClient kisWebSocketClient;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith(DESTINATION_PREFIX)) {
            String symbol = destination.substring(DESTINATION_PREFIX.length());
            log.info("Frontend subscribed to quotes for {}", symbol);
            kisWebSocketClient.subscribe(symbol);
        }
    }
}
