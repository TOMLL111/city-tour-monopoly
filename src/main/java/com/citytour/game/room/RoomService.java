package com.citytour.game.room;

import com.citytour.game.domain.Board;
import com.citytour.game.domain.GameEngine;
import com.citytour.game.domain.GameException;
import com.citytour.game.domain.GamePhase;
import com.citytour.game.domain.GameSettings;
import com.citytour.game.domain.GameState;
import com.citytour.game.domain.PlayerState;
import com.citytour.game.domain.TurnStep;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, RoomGame> rooms = new ConcurrentHashMap<>();
    private final GameEngine engine;
    private volatile java.util.function.Consumer<String> updateListener = ignored -> { };

    public RoomService(GameEngine engine) {
        this.engine = engine;
    }

    public void setUpdateListener(java.util.function.Consumer<String> updateListener) {
        this.updateListener = updateListener;
    }

    public RoomCredentials createRoom(String nickname) {
        String cleanName = validateNickname(nickname);
        String code;
        do {
            code = randomCode();
        } while (rooms.containsKey(code));
        PlayerState host = new PlayerState(UUID.randomUUID().toString(), cleanName, newToken(), true);
        RoomGame room = new RoomGame(code, host);
        rooms.put(code, room);
        return credentials(room, host);
    }

    public RoomCredentials joinRoom(String roomCode, String nickname) {
        RoomGame room = requireRoom(roomCode);
        String cleanName = validateNickname(nickname);
        synchronized (room) {
            if (room.getGameState().getPhase() != GamePhase.LOBBY) throw new GameException("GAME_STARTED", "对局已经开始");
            if (room.getPlayers().size() >= 7) throw new GameException("ROOM_FULL", "房间人数已满");
            boolean duplicate = room.getPlayers().stream().anyMatch(player -> player.getNickname().equalsIgnoreCase(cleanName));
            if (duplicate) throw new GameException("NICKNAME_TAKEN", "房间内已有同名玩家");
            PlayerState player = new PlayerState(UUID.randomUUID().toString(), cleanName, newToken(), false);
            room.getPlayers().add(player);
            room.getGameState().setVersion(room.getGameState().getVersion() + 1);
            room.addEvent("PLAYER_JOINED", player.getId(), cleanName + " 加入了房间", null);
            return credentials(room, player);
        }
    }

    public PlayerState authenticate(String roomCode, String token) {
        RoomGame room = requireRoom(roomCode);
        return room.getPlayers().stream()
                .filter(player -> secureEquals(player.getToken(), token))
                .findFirst()
                .orElseThrow(() -> new GameException("AUTH_FAILED", "会话已失效，请重新加入房间"));
    }

    public RoomSnapshot snapshot(String roomCode) {
        RoomGame room = requireRoom(roomCode);
        return snapshot(room);
    }

    public RoomSnapshot markConnected(String roomCode, String playerId) {
        RoomGame room = requireRoom(roomCode);
        synchronized (room) {
            PlayerState player = room.player(playerId);
            player.setConnected(true);
            room.getGameState().setVersion(room.getGameState().getVersion() + 1);
            room.addEvent("CONNECTED", playerId, player.getNickname() + " 已连接", null);
            if (room.getGameState().getPhase() == GamePhase.PAUSED
                    && room.activePlayers().stream().allMatch(PlayerState::isConnected)) {
                room.getGameState().setPhase(GamePhase.PLAYING);
                room.addEvent("GAME_RESUMED", null, "所有玩家已重新连接，对局继续", null);
                refreshDeadline(room);
            }
            return snapshot(room);
        }
    }

    public RoomSnapshot markDisconnected(String roomCode, String playerId) {
        RoomGame room = findRoom(roomCode).orElse(null);
        if (room == null) return null;
        synchronized (room) {
            Optional<PlayerState> maybePlayer = room.findPlayer(playerId);
            if (maybePlayer.isEmpty()) return snapshot(room);
            PlayerState player = maybePlayer.get();
            if (player.isConnected()) {
                player.setConnected(false);
                if (room.getGameState().getPhase() == GamePhase.PLAYING) {
                    room.getGameState().setPhase(GamePhase.PAUSED);
                    room.addEvent("GAME_PAUSED", playerId, player.getNickname() + " 掉线，对局暂停", null);
                    room.getGameState().setTurnDeadline(null);
                } else {
                    room.addEvent("DISCONNECTED", playerId, player.getNickname() + " 已断开连接", null);
                }
                room.getGameState().setVersion(room.getGameState().getVersion() + 1);
            }
            return snapshot(room);
        }
    }

    public RoomGame requireRoom(String code) {
        return findRoom(code).orElseThrow(() -> new GameException("ROOM_NOT_FOUND", "房间不存在或已过期"));
    }

    private Optional<RoomGame> findRoom(String code) {
        if (code == null) return Optional.empty();
        return Optional.ofNullable(rooms.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public CommandResult execute(String roomCode, String playerId, String type, String commandId, JsonNode payload) {
        RoomGame room = requireRoom(roomCode);
        synchronized (room) {
            if (room.rememberCommand(commandId).isPresent()) {
                return new CommandResult(snapshot(room), true);
            }
            PlayerState player = room.player(playerId);
            if (!player.isConnected()) throw new GameException("NOT_CONNECTED", "连接尚未完成身份同步");
            String command = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
            switch (command) {
                case "SET_READY" -> setReady(room, player, booleanValue(payload, "ready"));
                case "SET_SETTINGS" -> updateSettings(room, player, intValue(payload, "turnTimeLimitSeconds"));
                case "START_GAME" -> {
                    requireHost(room, playerId);
                    engine.startGame(room);
                }
                case "KICK_PLAYER" -> kickPlayer(room, playerId, stringValue(payload, "playerId"));
                case "ROLL_DICE" -> engine.rollDice(room, playerId);
                case "BUY_PROPERTY" -> engine.buyProperty(room, playerId);
                case "DECLINE_PURCHASE" -> engine.declinePurchase(room, playerId);
                case "PLACE_BID" -> engine.placeBid(room, playerId, intValue(payload, "amount"));
                case "PASS_AUCTION" -> engine.passAuction(room, playerId);
                case "END_TURN" -> engine.endTurn(room, playerId);
                case "BUILD_HOUSE" -> engine.buildHouse(room, playerId, intValue(payload, "propertyIndex"));
                case "SELL_HOUSE" -> engine.sellHouse(room, playerId, intValue(payload, "propertyIndex"));
                case "MORTGAGE_PROPERTY" -> engine.mortgageProperty(room, playerId, intValue(payload, "propertyIndex"));
                case "UNMORTGAGE_PROPERTY" -> engine.unmortgageProperty(room, playerId, intValue(payload, "propertyIndex"));
                case "JAIL_ACTION" -> engine.jailAction(room, playerId, stringValue(payload, "action"));
                case "PROPOSE_TRADE" -> engine.proposeTrade(room, playerId,
                        stringValue(payload, "toPlayerId"), intList(payload, "offeredPropertyIndexes"),
                        intList(payload, "requestedPropertyIndexes"), intValue(payload, "offeredCash"),
                        intValue(payload, "requestedCash"), intValue(payload, "offeredJailCards"),
                        intValue(payload, "requestedJailCards"));
                case "RESPOND_TRADE" -> engine.respondTrade(room, playerId, booleanValue(payload, "accept"));
                case "CANCEL_TRADE" -> engine.cancelTrade(room, playerId);
                case "SETTLE_DEBT" -> engine.settleDebt(room, playerId);
                case "DECLARE_BANKRUPTCY" -> engine.declareBankruptcy(room, playerId);
                case "SEND_CHAT" -> sendChat(room, player, stringValue(payload, "text"), false);
                case "SEND_EMOTE" -> sendChat(room, player, stringValue(payload, "text"), true);
                case "LEAVE_ROOM" -> leaveRoom(room, playerId);
                default -> throw new GameException("UNKNOWN_COMMAND", "未知操作：" + command);
            }
            room.getGameState().setVersion(room.getGameState().getVersion() + 1);
            refreshDeadline(room);
            room.touch();
            return new CommandResult(snapshot(room), false);
        }
    }

    private void setReady(RoomGame room, PlayerState player, boolean ready) {
        if (room.getGameState().getPhase() != GamePhase.LOBBY) throw new GameException("INVALID_PHASE", "对局已经开始");
        player.setReady(ready);
        room.addEvent("READY_CHANGED", player.getId(),
                player.getNickname() + (ready ? " 已准备" : " 取消准备"), null);
    }

    private void updateSettings(RoomGame room, PlayerState player, int seconds) {
        if (!player.isHost()) throw new GameException("NOT_HOST", "只有房主可以修改设置");
        if (room.getGameState().getPhase() != GamePhase.LOBBY) throw new GameException("INVALID_PHASE", "对局已经开始");
        if (seconds != 0 && seconds != 30 && seconds != 60 && seconds != 120) {
            throw new GameException("INVALID_SETTING", "回合时间仅支持关闭、30、60 或 120 秒");
        }
        room.updateSettings(new GameSettings(seconds));
    }

    private void kickPlayer(RoomGame room, String hostId, String targetId) {
        requireHost(room, hostId);
        if (room.getGameState().getPhase() != GamePhase.LOBBY) throw new GameException("INVALID_PHASE", "对局中不能移除玩家");
        if (hostId.equals(targetId)) throw new GameException("CANNOT_KICK_SELF", "房主不能移除自己");
        PlayerState target = room.player(targetId);
        room.getPlayers().remove(target);
        room.addEvent("PLAYER_KICKED", targetId, target.getNickname() + " 被房主移出房间", null);
    }

    private void leaveRoom(RoomGame room, String playerId) {
        PlayerState player = room.player(playerId);
        if (room.getGameState().getPhase() == GamePhase.LOBBY) {
            room.getPlayers().remove(player);
            room.addEvent("PLAYER_LEFT", playerId, player.getNickname() + " 离开了房间", null);
            if (room.getPlayers().isEmpty()) {
                rooms.remove(room.getCode());
                return;
            }
            room.transferHostIfNeeded();
        } else {
            engine.forfeit(room, playerId);
            player.setConnected(false);
        }
    }

    private void sendChat(RoomGame room, PlayerState player, String text, boolean emote) {
        if (text == null || text.isBlank()) throw new GameException("EMPTY_CHAT", "消息不能为空");
        if (text.length() > (emote ? 16 : 200)) throw new GameException("CHAT_TOO_LONG", "消息长度超出限制");
        long recent = room.getChatMessages().stream()
                .filter(message -> message.playerId().equals(player.getId()))
                .filter(message -> message.timestamp().isAfter(Instant.now().minusSeconds(10)))
                .count();
        if (recent >= 5) throw new GameException("CHAT_RATE_LIMIT", "发送过于频繁，请稍后再试");
        String clean = filterMessage(text.trim());
        room.addChat(player.getId(), player.getNickname(), emote ? "EMOTE" : "TEXT", clean);
    }

    private void refreshDeadline(RoomGame room) {
        GameState game = room.getGameState();
        int seconds = game.getSettings().turnTimeLimitSeconds();
        if (game.getPhase() == GamePhase.PLAYING && seconds > 0) {
            game.setTurnDeadline(Instant.now().plusSeconds(seconds));
        } else {
            game.setTurnDeadline(null);
        }
    }

    @Scheduled(fixedDelay = 1_000L)
    public void processTurnTimeouts() {
        Instant now = Instant.now();
        rooms.values().forEach(room -> {
            if (room.getGameState().getPhase() != GamePhase.PLAYING
                    || room.getGameState().getTurnDeadline() == null
                    || room.getGameState().getTurnDeadline().isAfter(now)) return;
            synchronized (room) {
                try {
                    engine.handleTimeout(room);
                    room.getGameState().setVersion(room.getGameState().getVersion() + 1);
                    refreshDeadline(room);
                    room.touch();
                    updateListener.accept(room.getCode());
                } catch (GameException ignored) {
                }
            }
        });
    }
    private RoomSnapshot snapshot(RoomGame room) {
        return new RoomSnapshot(room.getCode(), room.getHostId(), room.getGameState().getVersion(),
                List.copyOf(room.getPlayers()), Board.spaces(), Map.copyOf(room.getProperties()),
                room.getGameState(), List.copyOf(room.getEvents()), List.copyOf(room.getChatMessages()));
    }

    private RoomCredentials credentials(RoomGame room, PlayerState player) {
        return new RoomCredentials(room.getCode(), player.getId(), player.getToken(), snapshot(room));
    }

    private void requireHost(RoomGame room, String playerId) {
        if (!playerId.equals(room.getHostId())) throw new GameException("NOT_HOST", "只有房主可以执行该操作");
    }

    private String validateNickname(String nickname) {
        if (nickname == null) throw new GameException("INVALID_NICKNAME", "请输入昵称");
        String clean = nickname.replaceAll("[\\p{Cntrl}]", "").trim().replaceAll("\\s+", " ");
        if (clean.isEmpty() || clean.length() > 16) throw new GameException("INVALID_NICKNAME", "昵称长度需为 1 到 16 个字符");
        return clean;
    }

    private String filterMessage(String text) {
        String clean = text;
        for (String blocked : List.of("傻逼", "操你", "fuck", "shit")) {
            clean = clean.replaceAll("(?i)" + java.util.regex.Pattern.quote(blocked), "***");
        }
        return clean;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) builder.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        return builder.toString();
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean secureEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String stringValue(JsonNode payload, String field) {
        if (payload == null || payload.get(field) == null || payload.get(field).isNull()) return null;
        return payload.get(field).asText();
    }

    private int intValue(JsonNode payload, String field) {
        if (payload == null || payload.get(field) == null || !payload.get(field).canConvertToInt()) return 0;
        return payload.get(field).asInt();
    }

    private boolean booleanValue(JsonNode payload, String field) {
        return payload != null && payload.get(field) != null && payload.get(field).asBoolean(false);
    }

    private List<Integer> intList(JsonNode payload, String field) {
        List<Integer> values = new ArrayList<>();
        if (payload == null || payload.get(field) == null || !payload.get(field).isArray()) return values;
        payload.get(field).forEach(item -> values.add(item.asInt()));
        return values;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void cleanupExpiredRooms() {
        Instant now = Instant.now();
        rooms.entrySet().removeIf(entry -> {
            RoomGame room = entry.getValue();
            if (room.getPlayers().isEmpty()) return true;
            boolean emptyLobby = room.getGameState().getPhase() == GamePhase.LOBBY
                    && room.getPlayers().stream().noneMatch(PlayerState::isConnected)
                    && Duration.between(room.getLastActivity(), now).toMinutes() >= 30;
            boolean finishedExpired = room.getGameState().getPhase() == GamePhase.FINISHED
                    && Duration.between(room.getLastActivity(), now).toMinutes() >= 10;
            return emptyLobby || finishedExpired;
        });
    }
}

