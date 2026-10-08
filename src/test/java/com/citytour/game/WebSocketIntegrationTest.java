package com.citytour.game;

import com.citytour.game.room.RoomCredentials;
import com.citytour.game.room.RoomService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoomService roomService;

    @Test
    void authenticatesAndReceivesSnapshot() throws Exception {
        RoomCredentials credentials = roomService.createRoom("集成测试");
        String auth = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("type", "AUTH")
                .put("commandId", "integration-auth")
                .set("payload", objectMapper.createObjectNode()
                        .put("roomCode", credentials.roomCode())
                        .put("playerToken", credentials.playerToken())));
        CompletableFuture<JsonNode> snapshotFuture = new CompletableFuture<>();
        StringBuilder buffer = new StringBuilder();
        HttpClient httpClient = HttpClient.newHttpClient();
        WebSocket socket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws/game"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        webSocket.sendText(auth, true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            try {
                                JsonNode node = objectMapper.readTree(buffer.toString());
                                if ("SNAPSHOT".equals(node.path("type").asText())) snapshotFuture.complete(node);
                                buffer.setLength(0);
                            } catch (Exception exception) {
                                snapshotFuture.completeExceptionally(exception);
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        snapshotFuture.completeExceptionally(error);
                    }
                }).join();

        JsonNode received = snapshotFuture.get(5, TimeUnit.SECONDS);
        assertThat(received.path("snapshot").path("roomCode").asText()).isEqualTo(credentials.roomCode());
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }
}