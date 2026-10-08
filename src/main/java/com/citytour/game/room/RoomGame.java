package com.citytour.game.room;

import com.citytour.game.domain.Board;
import com.citytour.game.domain.CardActionType;
import com.citytour.game.domain.GameException;
import com.citytour.game.domain.GamePhase;
import com.citytour.game.domain.GameSettings;
import com.citytour.game.domain.GameState;
import com.citytour.game.domain.PlayerState;
import com.citytour.game.domain.PropertyState;
import com.citytour.game.domain.TourCard;
import com.citytour.game.domain.TradeOffer;
import com.citytour.game.domain.TurnStep;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RoomGame {
    private final String code;
    private final String hostId;
    private final Instant createdAt = Instant.now();
    private Instant lastActivity = Instant.now();
    private final List<PlayerState> players = new ArrayList<>();
    private final GameState gameState = new GameState();
    private final Map<Integer, PropertyState> properties = new HashMap<>();
    private final LinkedList<TourCard> chanceDeck = new LinkedList<>();
    private final LinkedList<TourCard> fateDeck = new LinkedList<>();
    private final List<RoomEvent> events = new ArrayList<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    @JsonIgnore
    private final Set<String> processedCommands = new HashSet<>();
    private long nextEventSequence = 1;
    private long nextChatSequence = 1;

    public RoomGame(String code, PlayerState host) {
        this.code = code;
        this.hostId = host.getId();
        this.players.add(host);
        Board.spaces().stream().filter(space -> space.isPurchasable())
                .forEach(space -> properties.put(space.index(), new PropertyState(space.index())));
        buildDecks();
        addEvent("ROOM_CREATED", host.getId(), host.getNickname() + " 创建了房间", null);
    }

    public String getCode() { return code; }
    public String getHostId() { return hostId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActivity() { return lastActivity; }
    public void touch() { lastActivity = Instant.now(); }
    public List<PlayerState> getPlayers() { return players; }
    public GameState getGameState() { return gameState; }
    public Map<Integer, PropertyState> getProperties() { return properties; }
    public List<TourCard> getChanceDeck() { return chanceDeck; }
    public List<TourCard> getFateDeck() { return fateDeck; }
    public List<RoomEvent> getEvents() { return events; }
    public List<ChatMessage> getChatMessages() { return chatMessages; }

    public PlayerState player(String playerId) {
        return players.stream().filter(player -> player.getId().equals(playerId)).findFirst()
                .orElseThrow(() -> new GameException("PLAYER_NOT_FOUND", "玩家不存在或已离开房间"));
    }

    public Optional<PlayerState> findPlayer(String playerId) {
        return players.stream().filter(player -> player.getId().equals(playerId)).findFirst();
    }

    public PlayerState currentPlayer() {
        if (gameState.getTurnPlayerId() == null) {
            throw new GameException("NOT_STARTED", "对局尚未开始");
        }
        return player(gameState.getTurnPlayerId());
    }

    public PropertyState property(int index) {
        PropertyState property = properties.get(index);
        if (property == null) {
            throw new GameException("NOT_PROPERTY", "该位置不是可购买地产");
        }
        return property;
    }

    public List<PlayerState> activePlayers() {
        return players.stream().filter(player -> !player.isBankrupt()).toList();
    }

    public List<PlayerState> connectedPlayers() {
        return players.stream().filter(player -> !player.isBankrupt() && player.isConnected()).toList();
    }

    public List<Integer> propertyIndexesOwnedBy(String playerId) {
        return properties.values().stream()
                .filter(property -> playerId.equals(property.getOwnerId()))
                .map(PropertyState::getIndex)
                .sorted()
                .toList();
    }

    public boolean ownsWholeGroup(String playerId, String group) {
        List<Integer> indexes = Board.colorGroup(group).stream().map(space -> space.index()).toList();
        return !indexes.isEmpty() && indexes.stream()
                .allMatch(index -> playerId.equals(properties.get(index).getOwnerId()));
    }

    public boolean hasBuildingsInGroup(String playerId, String group) {
        return Board.colorGroup(group).stream()
                .filter(space -> space.isPurchasable())
                .map(space -> properties.get(space.index()))
                .anyMatch(property -> property.getHouses() > 0 && playerId.equals(property.getOwnerId()));
    }

    public int countOwnedType(String playerId, com.citytour.game.domain.SpaceType type) {
        return Board.spaces().stream()
                .filter(space -> space.type() == type)
                .filter(space -> playerId.equals(properties.get(space.index()).getOwnerId()))
                .toList().size();
    }

    public void addEvent(String type, String actorId, String message, Integer amount) {
        events.add(new RoomEvent(nextEventSequence++, Instant.now(), type, actorId, message, amount));
        if (events.size() > 120) {
            events.subList(0, events.size() - 120).clear();
        }
        touch();
    }

    public void addChat(String playerId, String playerName, String type, String text) {
        chatMessages.add(new ChatMessage(nextChatSequence++, Instant.now(), playerId, playerName, type, text));
        if (chatMessages.size() > 100) {
            chatMessages.subList(0, chatMessages.size() - 100).clear();
        }
        touch();
    }

    public Optional<String> rememberCommand(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            return Optional.empty();
        }
        if (!processedCommands.add(commandId)) {
            return Optional.of(commandId);
        }
        if (processedCommands.size() > 500) {
            processedCommands.clear();
            processedCommands.add(commandId);
        }
        return Optional.empty();
    }

    public void resetForNewGame() {
        properties.values().forEach(property -> {
            property.setOwnerId(null);
            property.setMortgaged(false);
            property.setHouses(0);
        });
        players.forEach(player -> {
            player.setBankrupt(false);
            player.setPosition(0);
            player.setCash(1500);
            player.setInJail(false);
            player.setJailTurns(0);
            player.setGetOutOfJailCards(0);
        });
        gameState.setPhase(GamePhase.LOBBY);
        gameState.setTurnStep(TurnStep.WAIT_ROLL);
        gameState.setTurnPlayerId(null);
        gameState.setDiceOne(0);
        gameState.setDiceTwo(0);
        gameState.setDoublesCount(0);
        gameState.setLastRollWasDoubles(false);
        gameState.setPendingPurchaseIndex(null);
        gameState.setAuction(null);
        gameState.setTrade(null);
        gameState.setDebt(null);
        gameState.setWinnerId(null);
        gameState.setLastCardTitle(null);
        gameState.setLastCardText(null);
        gameState.setVersion(gameState.getVersion() + 1);
    }

    public void updateSettings(GameSettings settings) {
        gameState.setSettings(settings);
        gameState.setVersion(gameState.getVersion() + 1);
        addEvent("SETTINGS_UPDATED", hostId, "房主更新了回合时间设置", settings.turnTimeLimitSeconds());
    }

    public void transferHostIfNeeded() {
        if (players.stream().noneMatch(PlayerState::isHost) && !players.isEmpty()) {
            PlayerState next = players.stream().sorted(Comparator.comparing(PlayerState::getId)).findFirst().orElseThrow();
            next.setHost(true);
        }
    }

    private void buildDecks() {
        chanceDeck.add(new TourCard("CH01", "直飞出发地", "回到起点并领取 200 旅行币", CardActionType.MOVE_TO, 0, 0, 0, 0));
        chanceDeck.add(new TourCard("CH02", "高铁优惠", "前进 5 格", CardActionType.MOVE_BY, 5, 0, 0, 0));
        chanceDeck.add(new TourCard("CH03", "旅行补贴", "获得 100 旅行币", CardActionType.CREDIT, 100, 0, 0, 0));
        chanceDeck.add(new TourCard("CH04", "行李超重", "支付 50 旅行币", CardActionType.DEBIT, 50, 0, 0, 0));
        chanceDeck.add(new TourCard("CH05", "迷失方向", "后退 3 格", CardActionType.MOVE_BY, -3, 0, 0, 0));
        chanceDeck.add(new TourCard("CH06", "景区维修", "每栋房屋支付 25，每座酒店支付 100", CardActionType.REPAIRS, 0, 0, 25, 100));
        chanceDeck.add(new TourCard("CH07", "临时通行证", "获得一张出狱卡", CardActionType.GET_OUT_OF_JAIL, 0, 0, 0, 0));
        chanceDeck.add(new TourCard("CH08", "走错口岸", "直接进入城市监狱", CardActionType.GO_TO_JAIL, 0, 0, 0, 0));
        chanceDeck.add(new TourCard("CH09", "直达外滩", "前往上海外滩", CardActionType.MOVE_TO, 0, 37, 0, 0));
        chanceDeck.add(new TourCard("CH10", "摄影大奖", "获得 150 旅行币", CardActionType.CREDIT, 150, 0, 0, 0));
        chanceDeck.add(new TourCard("CH11", "长途改签", "前往最近的北京站", CardActionType.MOVE_TO, 0, 5, 0, 0));
        chanceDeck.add(new TourCard("CH12", "城市公益", "向每名玩家支付 30 旅行币", CardActionType.DEBIT, 30, 0, 0, 0));

        fateDeck.add(new TourCard("FA01", "银行分红", "获得 50 旅行币", CardActionType.CREDIT, 50, 0, 0, 0));
        fateDeck.add(new TourCard("FA02", "医疗费用", "支付 50 旅行币", CardActionType.DEBIT, 50, 0, 0, 0));
        fateDeck.add(new TourCard("FA03", "继承旅行基金", "获得 100 旅行币", CardActionType.CREDIT, 100, 0, 0, 0));
        fateDeck.add(new TourCard("FA04", "教育投资", "支付 100 旅行币", CardActionType.DEBIT, 100, 0, 0, 0));
        fateDeck.add(new TourCard("FA05", "回到杭州西湖", "前往杭州西湖", CardActionType.MOVE_TO, 0, 21, 0, 0));
        fateDeck.add(new TourCard("FA06", "前往出发地", "回到起点并领取 200 旅行币", CardActionType.MOVE_TO, 0, 0, 0, 0));
        fateDeck.add(new TourCard("FA07", "交通罚单", "支付 40 旅行币", CardActionType.DEBIT, 40, 0, 0, 0));
        fateDeck.add(new TourCard("FA08", "城市奖励", "获得 75 旅行币", CardActionType.CREDIT, 75, 0, 0, 0));
        fateDeck.add(new TourCard("FA09", "社区维修", "每栋房屋支付 20，每座酒店支付 80", CardActionType.REPAIRS, 0, 0, 20, 80));
        fateDeck.add(new TourCard("FA10", "法院传票", "直接进入城市监狱", CardActionType.GO_TO_JAIL, 0, 0, 0, 0));
        fateDeck.add(new TourCard("FA11", "旅行券", "获得一张出狱卡", CardActionType.GET_OUT_OF_JAIL, 0, 0, 0, 0));
        fateDeck.add(new TourCard("FA12", "返程延误", "后退 2 格", CardActionType.MOVE_BY, -2, 0, 0, 0));
    }
}

