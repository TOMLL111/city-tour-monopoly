package com.citytour.game.ws;

import com.citytour.game.domain.GameException;
import com.citytour.game.domain.PlayerState;
import com.citytour.game.room.RoomService;
import com.citytour.game.room.RoomSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final RoomService roomService;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> roomConnections = new ConcurrentHashMap<>();

    public GameWebSocketHandler(ObjectMapper objectMapper, RoomService roomService) {
        this.objectMapper = objectMapper;
        this.roomService = roomService;
        roomService.setUpdateListener(this::broadcast);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(32_768);
        session.setBinaryMessageSizeLimit(32_768);
        connections.put(session.getId(), new Connection(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Connection connection = connections.get(session.getId());
        if (connection == null) return;
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = text(root, "type").toUpperCase();
            String commandId = text(root, "commandId");
            JsonNode payload = root.get("payload");
            if (connection.playerId == null) {
                if (!"AUTH".equals(type)) throw new GameException("AUTH_REQUIRED", "请先完成连接认证");
                authenticate(connection, payload);
                return;
            }
            if ("PING".equals(type)) {
                send(session, Map.of("type", "PONG"));
                return;
            }
            var result = roomService.execute(connection.roomCode, connection.playerId, type, commandId, payload);
            if (!result.duplicate()) broadcast(connection.roomCode);
        } catch (GameException exception) {
            sendError(session, exception.getCode(), exception.getMessage());
        } catch (Exception exception) {
            sendError(session, "BAD_MESSAGE", "消息格式不正确");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Connection connection = connections.remove(session.getId());
        if (connection == null || connection.roomCode == null) return;
        Set<String> sessions = roomConnections.get(connection.roomCode);
        if (sessions != null) sessions.remove(session.getId());
        boolean stillConnected = sessions != null && sessions.stream().map(connections::get)
                .filter(java.util.Objects::nonNull)
                .anyMatch(item -> connection.playerId.equals(item.playerId));
        if (!stillConnected) {
            RoomSnapshot snapshot = roomService.markDisconnected(connection.roomCode, connection.playerId);
            if (snapshot != null) broadcast(connection.roomCode);
        }
    }

    private void authenticate(Connection connection, JsonNode payload) throws IOException {
        String roomCode = text(payload, "roomCode").toUpperCase();
        String playerToken = text(payload, "playerToken");
        PlayerState player = roomService.authenticate(roomCode, playerToken);
        connection.roomCode = roomCode;
        connection.playerId = player.getId();
        roomConnections.computeIfAbsent(roomCode, ignored -> ConcurrentHashMap.newKeySet()).add(connection.session.getId());
        RoomSnapshot snapshot = roomService.markConnected(roomCode, player.getId());
        sendSnapshot(connection.session, player.getId(), snapshot);
        broadcast(roomCode);
    }

    public void broadcast(String roomCode) {
        Set<String> sessions = roomConnections.get(roomCode);
        if (sessions == null || sessions.isEmpty()) return;
        RoomSnapshot snapshot;
        try {
            snapshot = roomService.snapshot(roomCode);
        } catch (GameException exception) {
            return;
        }
        sessions.stream().map(connections::get).filter(java.util.Objects::nonNull).forEach(connection -> {
            try {
                sendSnapshot(connection.session, connection.playerId, snapshot);
            } catch (IOException ignored) {
            }
        });
    }

    private void sendSnapshot(WebSocketSession session, String playerId, RoomSnapshot snapshot) throws IOException {
        send(session, Map.of(
                "type", "SNAPSHOT",
                "playerId", playerId,
                "version", snapshot.version(),
                "snapshot", snapshot
        ));
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            send(session, Map.of("type", "ERROR", "code", code, "message", message == null ? "操作失败" : message));
        } catch (IOException ignored) {
        }
    }

    private void send(WebSocketSession session, Object value) throws IOException {
        if (!session.isOpen()) return;
        String payload = objectMapper.writeValueAsString(value);
        synchronized (session) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return "";
        return node.get(field).asText();
    }

    private static class Connection {
        private final WebSocketSession session;
        private String roomCode;
        private String playerId;

        private Connection(WebSocketSession session) {
            this.session = session;
        }
    }
}
