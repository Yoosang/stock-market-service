package com.usang.stockmarket.infra.kis;

import com.usang.stockmarket.domain.watchlist.Watchlist;
import com.usang.stockmarket.domain.watchlist.WatchlistRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisWebSocketClient extends TextWebSocketHandler {
    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final KisAuthClient kisAuthClient;
    private final KisConfiguration kisConfiguration;
    private final ObjectMapper objectMapper;
    private final WatchlistRepository watchlistRepository;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private WebSocketSession kisWebSocketSession;

    @PostConstruct
    public void connect() {
        new StandardWebSocketClient().execute(this, kisConfiguration.wsUri());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(1024 * 1024); // KIS가 여러 체결가를 한 프레임에 묶어 보내는 경우가 있어 기본 버퍼(8KB)로는 부족함
        kisWebSocketSession = session;
        log.info("KIS WebSocket connected.");

        List<String> stocklist = watchlistRepository
                .findAll()
                .stream()
                .map(Watchlist::getStockSymbol)
                .distinct()
                .toList();
        
        for (String stockCode : stocklist) {
            subscribe(stockCode);
        }

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.startsWith("{")) {
            handleControlMessage(session, payload);
        } else {
            handleTickData(payload);
        }
    }

    private void handleControlMessage(WebSocketSession session, String payload) throws Exception {
        Map<String, Object> parsed = objectMapper.readValue(payload, Map.class);
        Object headerObj = parsed.get("header");
        String trId = headerObj instanceof Map<?, ?> header ? String.valueOf(header.get("tr_id")) : null;

        if ("PINGPONG".equals(trId)) {
            // KIS는 PINGPONG을 받은 그대로 돌려보내지 않으면 몇 번 안에 연결을 끊는다 (특히 장 마감 이후)
            session.sendMessage(new TextMessage(payload));
            log.debug("KIS PINGPONG replied.");
            return;
        }

        log.info("KIS control message: {}", payload);
    }

    private void handleTickData(String payload) {
        //TODO frontend 보내주는 로직 추가 예정 — 지금은 구조 확인 차원에서 tr_id/건수/첫 종목코드만 로그
        String[] parts = payload.split("\\|", 4);
        if (parts.length < 4) {
            log.warn("Unexpected KIS tick payload: {}", payload);
            return;
        }
        String trId = parts[1];
        String count = parts[2];
        String firstSymbol = parts[3].split("\\^")[0];
        log.info("KIS tick data: trId={}, count={}, firstSymbol={}", trId, count, firstSymbol);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("KIS WebSocket disconnected ({}). Reconnecting in {}s...", status, RECONNECT_DELAY_SECONDS);
        reconnectExecutor.schedule(this::connect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public void subscribe(String stockCode) {
        sendMessage(stockCode, "1");
    }

    public void unsubscribe(String stockCode) {
        sendMessage(stockCode, "2");
    }

    private void sendMessage(String stockCode, String trType) {
        try {
            Map<String, Object> message = Map.of(
                    "header", Map.of(
                            "approval_key", kisAuthClient.getNewApprovalKey(),
                            "custtype", "P",
                            "tr_type", trType,
                            "content-type", "utf-8"
                    ),
                    "body", Map.of(
                            "input", Map.of(
                                    "tr_id", "H0UNCNT0",
                                    "tr_key", stockCode
                            )
                    )
            );
            kisWebSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            log.info("KIS {} to: {}", trType.equals("1") ? "subscribed" : "unsubscribed", stockCode);
        }
        catch (Exception e) {
            log.error("KIS sendMessage failed for {}: {}", stockCode, e.getMessage());
        }

    }
}