package com.usang.stockmarket.global.config;

import com.usang.stockmarket.infra.security.JwtAuthenticationResolver;
import com.usang.stockmarket.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 프론트엔드 없이, 실제 STOMP 클라이언트로 우리 서버의 /ws에 접속해서
 * JWT 인증(WebSocket 핸드셰이크 단계의 쿠키 검증)이 실제로 동작하는지 확인하는 테스트.
 * 앱 전체 컨텍스트가 뜨므로 로컬 Postgres(docker compose)가 실행 중이어야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteStompIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 유효하지_않은_토큰이면_STOMP_연결이_거부된다() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Cookie", JwtAuthenticationResolver.COOKIE_NAME + "=invalid-token");

        CompletableFuture<StompSession> future = new CompletableFuture<>();
        stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                handshakeHeaders,
                new StompHeaders(),
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders headers) {
                        future.complete(session);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        future.completeExceptionally(exception);
                    }
                });

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS));
        System.out.println("expected rejection: " + exception.getCause());
    }

    @Test
    void 유효한_토큰이면_STOMP_연결에_성공한다() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        String token = jwtTokenProvider.generateToken(1L);
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Cookie", JwtAuthenticationResolver.COOKIE_NAME + "=" + token);

        CompletableFuture<StompSession> future = new CompletableFuture<>();
        stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                handshakeHeaders,
                new StompHeaders(),
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders headers) {
                        future.complete(session);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        future.completeExceptionally(exception);
                    }
                });

        StompSession session = future.get(5, TimeUnit.SECONDS);
        assertTrue(session.isConnected());
        session.disconnect();
    }
}
