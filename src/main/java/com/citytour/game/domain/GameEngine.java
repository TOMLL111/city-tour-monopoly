package com.citytour.game.domain;

import com.citytour.game.room.RoomGame;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Component
public class GameEngine {
    private final SecureRandom random = new SecureRandom();

    public void startGame(RoomGame room) {
        GameState game = room.getGameState();
        var players = room.getPlayers();
        if (game.getPhase() != GamePhase.LOBBY) throw new GameException("INVALID_PHASE", "当前不在等待大厅");
        if (players.size() < 3 || players.size() > 7) throw new GameException("PLAYER_COUNT", "游戏需要 3 到 7 名玩家");
        if (players.stream().anyMatch(player -> !player.isReady())) {
            throw new GameException("PLAYERS_NOT_READY", "所有玩家准备后才能开始");
        }
        room.resetForNewGame();
        game.setPhase(GamePhase.PLAYING);
        game.setTurnStep(TurnStep.WAIT_ROLL);
        game.setTurnPlayerId(players.get(0).getId());
        game.setVersion(game.getVersion() + 1);
        room.addEvent("GAME_STARTED", players.get(0).getId(),
                "城市环游记开始，由 " + players.get(0).getNickname() + " 先手", null);
    }

    public void rollDice(RoomGame room, String playerId) {
        assertPlaying(room);
        assertTurn(room, playerId);
        GameState game = room.getGameState();
        PlayerState player = room.player(playerId);
        if (player.isInJail()) {
            game.setTurnStep(TurnStep.WAIT_JAIL_DECISION);
            return;
        }
        if (game.getTurnStep() != TurnStep.WAIT_ROLL) throw new GameException("INVALID_STEP", "当前不能掷骰");
        int first = random.nextInt(6) + 1;
        int second = random.nextInt(6) + 1;
        game.setDiceOne(first);
        game.setDiceTwo(second);
        boolean doubles = first == second;
        game.setLastRollWasDoubles(doubles);
        game.setDoublesCount(doubles ? game.getDoublesCount() + 1 : 0);
        room.addEvent("DICE_ROLLED", playerId,
                player.getNickname() + " 掷出 " + first + " + " + second + " = " + (first + second), first + second);
        if (doubles && game.getDoublesCount() >= 3) {
            sendToJail(room, player, "连续三次掷出双骰");
            game.setTurnStep(TurnStep.WAIT_END_TURN);
            return;
        }
        moveAndResolve(room, player, first + second, true);
    }

    public void jailAction(RoomGame room, String playerId, String action) {
        assertPlaying(room);
        assertTurn(room, playerId);
        PlayerState player = room.player(playerId);
        GameState game = room.getGameState();
        if (!player.isInJail() || game.getTurnStep() != TurnStep.WAIT_JAIL_DECISION) {
            throw new GameException("NOT_IN_JAIL", "当前不需要处理监狱");
        }
        switch (action) {
            case "PAY" -> {
                room.addEvent("JAIL_PAY", playerId, player.getNickname() + " 支付 50 旅行币离开监狱", 50);
                charge(room, player, null, 50, "监狱保释");
                if (game.getTurnStep() != TurnStep.DEBT_RESOLUTION) {
                    player.setInJail(false);
                    player.setJailTurns(0);
                    game.setTurnStep(TurnStep.WAIT_ROLL);
                }
            }
            case "CARD" -> {
                if (player.getGetOutOfJailCards() <= 0) throw new GameException("NO_JAIL_CARD", "没有可用的出狱卡");
                player.setGetOutOfJailCards(player.getGetOutOfJailCards() - 1);
                player.setInJail(false);
                player.setJailTurns(0);
                game.setTurnStep(TurnStep.WAIT_ROLL);
                room.addEvent("JAIL_CARD", playerId, player.getNickname() + " 使用出狱卡离开监狱", null);
            }
            case "ROLL" -> rollFromJail(room, player);
            default -> throw new GameException("INVALID_ACTION", "未知的监狱操作");
        }
    }

    private void rollFromJail(RoomGame room, PlayerState player) {
        GameState game = room.getGameState();
        int first = random.nextInt(6) + 1;
        int second = random.nextInt(6) + 1;
        game.setDiceOne(first);
        game.setDiceTwo(second);
        room.addEvent("JAIL_ROLL", player.getId(),
                player.getNickname() + " 在监狱掷出 " + first + " + " + second, first + second);
        if (first == second) {
            player.setInJail(false);
            player.setJailTurns(0);
            game.setLastRollWasDoubles(true);
            game.setDoublesCount(1);
            moveAndResolve(room, player, first + second, true);
            return;
        }
        player.setJailTurns(player.getJailTurns() + 1);
        if (player.getJailTurns() >= 3) {
            room.addEvent("JAIL_FINE", player.getId(),
                    player.getNickname() + " 第三次未掷出双骰，缴纳 50 旅行币", 50);
            player.setInJail(false);
            player.setJailTurns(0);
            charge(room, player, null, 50, "第三次监狱罚款");
            if (game.getTurnStep() != TurnStep.DEBT_RESOLUTION) game.setTurnStep(TurnStep.WAIT_END_TURN);
        } else {
            game.setLastRollWasDoubles(false);
            game.setTurnStep(TurnStep.WAIT_END_TURN);
        }
    }

    private void moveAndResolve(RoomGame room, PlayerState player, int steps, boolean collectGo) {
        int oldPosition = player.getPosition();
        int next = Math.floorMod(oldPosition + steps, 40);
        if (collectGo && steps > 0 && next < oldPosition) {
            player.setCash(player.getCash() + 200);
            room.addEvent("PASS_GO", player.getId(), player.getNickname() + " 经过出发地，获得 200 旅行币", 200);
        }
        player.setPosition(next);
        resolveLanding(room, player, next);
    }

    private void resolveLanding(RoomGame room, PlayerState player, int position) {
        GameState game = room.getGameState();
        BoardSpace space = Board.space(position);
        room.addEvent("LANDED", player.getId(), player.getNickname() + " 抵达 " + space.name(), null);
        switch (space.type()) {
            case PROPERTY, RAILROAD, UTILITY -> resolveProperty(room, player, space);
            case TAX -> {
                room.addEvent("TAX", player.getId(),
                        player.getNickname() + " 缴纳 " + space.taxAmount() + " 旅行币", space.taxAmount());
                charge(room, player, null, space.taxAmount(), space.name());
            }
            case CHANCE -> drawCard(room, player, true);
            case FATE -> drawCard(room, player, false);
            case GO_TO_JAIL -> sendToJail(room, player, "抵达前往监狱格");
            default -> game.setTurnStep(TurnStep.WAIT_END_TURN);
        }
    }

    private void resolveProperty(RoomGame room, PlayerState player, BoardSpace space) {
        PropertyState property = room.property(space.index());
        GameState game = room.getGameState();
        if (property.getOwnerId() == null) {
            game.setPendingPurchaseIndex(space.index());
            game.setTurnStep(TurnStep.WAIT_PURCHASE);
            return;
        }
        if (property.getOwnerId().equals(player.getId()) || property.isMortgaged()) {
            game.setTurnStep(TurnStep.WAIT_END_TURN);
            return;
        }
        PlayerState owner = room.player(property.getOwnerId());
        int rent = calculateRent(room, space, property);
        room.addEvent("RENT", player.getId(),
                player.getNickname() + " 向 " + owner.getNickname() + " 支付 " + rent + " 旅行币租金", rent);
        charge(room, player, owner, rent, "租金：" + space.name());
    }

    public int calculateRent(RoomGame room, BoardSpace space, PropertyState property) {
        if (property.isMortgaged() || property.getOwnerId() == null) return 0;
        String ownerId = property.getOwnerId();
        return switch (space.type()) {
            case PROPERTY -> {
                int base = space.rentTable().get(Math.min(property.getHouses(), 5));
                boolean monopoly = room.ownsWholeGroup(ownerId, space.colorGroup());
                yield property.getHouses() == 0 && monopoly ? base * 2 : base;
            }
            case RAILROAD -> {
                int count = room.countOwnedType(ownerId, SpaceType.RAILROAD);
                yield space.rentTable().get(Math.max(0, Math.min(count - 1, 3)));
            }
            case UTILITY -> {
                int count = room.countOwnedType(ownerId, SpaceType.UTILITY);
                int multiplier = count >= 2 ? 10 : 4;
                yield (room.getGameState().getDiceOne() + room.getGameState().getDiceTwo()) * multiplier;
            }
            default -> 0;
        };
    }

    private void charge(RoomGame room, PlayerState debtor, PlayerState creditor, int amount, String reason) {
        if (amount <= 0) return;
        if (debtor.getCash() >= amount) {
            debtor.setCash(debtor.getCash() - amount);
            if (creditor != null) creditor.setCash(creditor.getCash() + amount);
            room.addEvent("PAYMENT", debtor.getId(),
                    debtor.getNickname() + " 支付 " + amount + " 旅行币（" + reason + "）", amount);
            return;
        }
        room.getGameState().setDebt(new DebtState(debtor.getId(),
                creditor == null ? null : creditor.getId(), amount, reason));
        room.getGameState().setTurnStep(TurnStep.DEBT_RESOLUTION);
        room.addEvent("DEBT", debtor.getId(),
                debtor.getNickname() + " 需要筹集 " + amount + " 旅行币（" + reason + "）", amount);
    }

    private void drawCard(RoomGame room, PlayerState player, boolean chance) {
        List<TourCard> deck = chance ? room.getChanceDeck() : room.getFateDeck();
        TourCard card = deck.remove(0);
        deck.add(card);
        GameState game = room.getGameState();
        game.setLastCardTitle(card.title());
        game.setLastCardText(card.text());
        room.addEvent("CARD_DRAWN", player.getId(),
                player.getNickname() + " 抽到「" + card.title() + "」：" + card.text(), null);
        if ("CH12".equals(card.id())) {
            for (PlayerState other : room.activePlayers()) {
                if (!other.getId().equals(player.getId())) {
                    charge(room, player, other, card.amount(), "城市公益");
                    if (game.getTurnStep() == TurnStep.DEBT_RESOLUTION) break;
                }
            }
            if (game.getTurnStep() != TurnStep.DEBT_RESOLUTION) game.setTurnStep(TurnStep.WAIT_END_TURN);
            return;
        }
        switch (card.action()) {
            case MOVE_TO -> {
                int destination = card.destination();
                if (destination < player.getPosition()) {
                    player.setCash(player.getCash() + 200);
                    room.addEvent("PASS_GO", player.getId(), player.getNickname() + " 经过出发地，获得 200 旅行币", 200);
                }
                player.setPosition(destination);
                resolveLanding(room, player, destination);
            }
            case MOVE_BY -> moveAndResolve(room, player, card.amount(), true);
            case CREDIT -> {
                player.setCash(player.getCash() + card.amount());
                game.setTurnStep(TurnStep.WAIT_END_TURN);
            }
            case DEBIT -> {
                charge(room, player, null, card.amount(), card.title());
                if (game.getTurnStep() != TurnStep.DEBT_RESOLUTION) game.setTurnStep(TurnStep.WAIT_END_TURN);
            }
            case GO_TO_JAIL -> {
                sendToJail(room, player, card.title());
                game.setTurnStep(TurnStep.WAIT_END_TURN);
            }
            case GET_OUT_OF_JAIL -> {
                player.setGetOutOfJailCards(player.getGetOutOfJailCards() + 1);
                game.setTurnStep(TurnStep.WAIT_END_TURN);
            }
            case REPAIRS -> {
                int houses = 0;
                int hotels = 0;
                for (int index : room.propertyIndexesOwnedBy(player.getId())) {
                    int buildingCount = room.property(index).getHouses();
                    if (buildingCount == 5) hotels++;
                    else houses += buildingCount;
                }
                int total = houses * card.houseRepair() + hotels * card.hotelRepair();
                if (total > 0) charge(room, player, null, total, card.title());
                if (game.getTurnStep() != TurnStep.DEBT_RESOLUTION) game.setTurnStep(TurnStep.WAIT_END_TURN);
            }
        }
    }

    private void sendToJail(RoomGame room, PlayerState player, String reason) {
        player.setPosition(10);
        player.setInJail(true);
        player.setJailTurns(0);
        player.setPosition(10);
        room.getGameState().setDoublesCount(0);
        room.getGameState().setLastRollWasDoubles(false);
        room.addEvent("GO_TO_JAIL", player.getId(), player.getNickname() + " 进入城市监狱（" + reason + "）", null);
    }

    public void buyProperty(RoomGame room, String playerId) {
        assertPlaying(room);
        assertTurn(room, playerId);
        GameState game = room.getGameState();
        if (game.getTurnStep() != TurnStep.WAIT_PURCHASE || game.getPendingPurchaseIndex() == null) {
            throw new GameException("NO_PURCHASE", "当前没有待购买地产");
        }
        PlayerState player = room.player(playerId);
        BoardSpace space = Board.space(game.getPendingPurchaseIndex());
        if (player.getCash() < space.price()) throw new GameException("INSUFFICIENT_CASH", "旅行币不足");
        player.setCash(player.getCash() - space.price());
        room.property(space.index()).setOwnerId(playerId);
        room.addEvent("PROPERTY_BOUGHT", playerId,
                player.getNickname() + " 以 " + space.price() + " 旅行币购买 " + space.name(), space.price());
        game.setPendingPurchaseIndex(null);
        game.setTurnStep(TurnStep.WAIT_END_TURN);
    }

    public void declinePurchase(RoomGame room, String playerId) {
        assertPlaying(room);
        assertTurn(room, playerId);
        GameState game = room.getGameState();
        if (game.getTurnStep() != TurnStep.WAIT_PURCHASE || game.getPendingPurchaseIndex() == null) {
            throw new GameException("NO_PURCHASE", "当前没有待购买地产");
        }
        int index = game.getPendingPurchaseIndex();
        game.setPendingPurchaseIndex(null);
        startAuction(room, index);
    }

    private void startAuction(RoomGame room, int propertyIndex) {
        List<String> active = room.activePlayers().stream().map(PlayerState::getId).toList();
        if (active.size() <= 1) {
            room.addEvent("AUCTION_SKIPPED", null,
                    Board.space(propertyIndex).name() + " 没有足够玩家参与拍卖，仍属于银行", null);
            room.getGameState().setTurnStep(TurnStep.WAIT_END_TURN);
            return;
        }
        String first = nextPlayerId(room, room.getGameState().getTurnPlayerId(), active);
        room.getGameState().setAuction(new AuctionState(propertyIndex, first, active));
        room.getGameState().setTurnStep(TurnStep.AUCTION);
        room.addEvent("AUCTION_STARTED", null,
                Board.space(propertyIndex).name() + " 开始拍卖", null);
    }

    public void placeBid(RoomGame room, String playerId, int amount) {
        assertPlaying(room);
        GameState game = room.getGameState();
        AuctionState auction = game.getAuction();
        if (game.getTurnStep() != TurnStep.AUCTION || auction == null) throw new GameException("NO_AUCTION", "当前没有拍卖");
        if (!playerId.equals(auction.getCurrentBidderId())) throw new GameException("NOT_YOUR_BID", "还未轮到你出价");
        PlayerState bidder = room.player(playerId);
        if (bidder.isBankrupt()) throw new GameException("BANKRUPT", "破产玩家不能参与拍卖");
        if (amount <= auction.getHighBid() || amount <= 0) throw new GameException("BID_TOO_LOW", "出价必须高于当前最高价");
        if (amount > bidder.getCash()) throw new GameException("INSUFFICIENT_CASH", "出价不能超过现有旅行币");
        auction.setHighBid(amount);
        auction.setHighBidderId(playerId);
        room.addEvent("BID_PLACED", playerId, bidder.getNickname() + " 出价 " + amount + " 旅行币", amount);
        if (auction.getActiveBidderIds().size() == 1) {
            completeAuction(room);
            return;
        }
        auction.setCurrentBidderId(nextPlayerId(room, playerId, auction.getActiveBidderIds()));
    }

    public void passAuction(RoomGame room, String playerId) {
        assertPlaying(room);
        GameState game = room.getGameState();
        AuctionState auction = game.getAuction();
        if (game.getTurnStep() != TurnStep.AUCTION || auction == null) throw new GameException("NO_AUCTION", "当前没有拍卖");
        if (!playerId.equals(auction.getCurrentBidderId())) throw new GameException("NOT_YOUR_BID", "还未轮到你放弃");
        auction.getActiveBidderIds().remove(playerId);
        room.addEvent("AUCTION_PASS", playerId, room.player(playerId).getNickname() + " 放弃本次拍卖", null);
        if (auction.getActiveBidderIds().isEmpty()
                || (auction.getActiveBidderIds().size() == 1
                && auction.getActiveBidderIds().get(0).equals(auction.getHighBidderId()))) {
            completeAuction(room);
            return;
        }
        auction.setCurrentBidderId(nextPlayerId(room, playerId, auction.getActiveBidderIds()));
    }

    private void completeAuction(RoomGame room) {
        GameState game = room.getGameState();
        AuctionState auction = game.getAuction();
        PropertyState property = room.property(auction.getPropertyIndex());
        if (auction.getHighBidderId() != null && auction.getHighBid() > 0) {
            PlayerState winner = room.player(auction.getHighBidderId());
            winner.setCash(winner.getCash() - auction.getHighBid());
            property.setOwnerId(winner.getId());
            property.setMortgaged(false);
            room.addEvent("AUCTION_WON", winner.getId(),
                    winner.getNickname() + " 以 " + auction.getHighBid() + " 旅行币拍得 "
                            + Board.space(property.getIndex()).name(), auction.getHighBid());
        } else {
            room.addEvent("AUCTION_UNSOLD", null,
                    Board.space(property.getIndex()).name() + " 流拍，仍属于银行", null);
        }
        game.setAuction(null);
        startQueuedAuctionOrEnd(room);
    }

    public void endTurn(RoomGame room, String playerId) {
        assertPlaying(room);
        assertTurn(room, playerId);
        GameState game = room.getGameState();
        if (game.getTurnStep() != TurnStep.WAIT_END_TURN) throw new GameException("INVALID_STEP", "当前不能结束回合");
        PlayerState player = room.player(playerId);
        if (game.isLastRollWasDoubles() && !player.isInJail()) {
            game.setLastRollWasDoubles(false);
            game.setTurnStep(TurnStep.WAIT_ROLL);
            room.addEvent("EXTRA_TURN", playerId, player.getNickname() + " 掷出双骰，可以再掷一次", null);
            return;
        }
        advanceTurn(room);
    }

    public void buildHouse(RoomGame room, String playerId, int propertyIndex) {
        assertPlaying(room);
        GameState game = room.getGameState();
        PlayerState player = room.player(playerId);
        assertCurrentForManagement(room, playerId);
        if (game.getTurnStep() == TurnStep.DEBT_RESOLUTION) throw new GameException("DEBT_ACTIVE", "债务阶段不能建房");
        BoardSpace space = Board.space(propertyIndex);
        PropertyState property = room.property(propertyIndex);
        if (space.type() != SpaceType.PROPERTY) throw new GameException("NOT_HOUSE_PROPERTY", "该地产不能建房");
        if (!playerId.equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "你不是该地产的拥有者");
        if (!room.ownsWholeGroup(playerId, space.colorGroup())) throw new GameException("NO_MONOPOLY", "拥有整组地产后才能建房");
        if (property.isMortgaged() || hasMortgagedInGroup(room, propertyIndex)) {
            throw new GameException("GROUP_MORTGAGED", "同组地产被抵押时不能建房");
        }
        if (property.getHouses() >= 5) throw new GameException("MAX_BUILDING", "该地产已建成酒店");
        if (player.getCash() < space.houseCost()) throw new GameException("INSUFFICIENT_CASH", "旅行币不足");
        if (!canBuildEvenly(room, space, property)) throw new GameException("EVEN_BUILD", "必须保持同组地产均衡建房");
        player.setCash(player.getCash() - space.houseCost());
        property.setHouses(property.getHouses() + 1);
        room.addEvent("BUILD", playerId,
                property.getHouses() == 5 ? player.getNickname() + " 在 " + space.name() + " 建成酒店"
                        : player.getNickname() + " 在 " + space.name() + " 建房至 " + property.getHouses() + " 级",
                space.houseCost());
    }

    public void sellHouse(RoomGame room, String playerId, int propertyIndex) {
        assertPlaying(room);
        assertCurrentForManagement(room, playerId);
        BoardSpace space = Board.space(propertyIndex);
        PropertyState property = room.property(propertyIndex);
        if (!playerId.equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "你不是该地产的拥有者");
        if (property.getHouses() <= 0) throw new GameException("NO_BUILDING", "该地产没有可出售的建筑");
        if (!canSellEvenly(room, space, property)) throw new GameException("EVEN_SELL", "必须保持同组地产均衡出售建筑");
        int proceeds = space.houseCost() / 2;
        property.setHouses(property.getHouses() - 1);
        room.player(playerId).setCash(room.player(playerId).getCash() + proceeds);
        room.addEvent("SELL_BUILDING", playerId,
                room.player(playerId).getNickname() + " 出售 " + space.name() + " 的建筑，获得 " + proceeds + " 旅行币",
                proceeds);
    }

    public void mortgageProperty(RoomGame room, String playerId, int propertyIndex) {
        assertPlaying(room);
        assertCurrentForManagement(room, playerId);
        BoardSpace space = Board.space(propertyIndex);
        PropertyState property = room.property(propertyIndex);
        if (!playerId.equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "你不是该地产的拥有者");
        if (property.isMortgaged()) throw new GameException("ALREADY_MORTGAGED", "该地产已经抵押");
        if (room.hasBuildingsInGroup(playerId, space.colorGroup())) {
            throw new GameException("GROUP_HAS_BUILDINGS", "必须先出售同组地产上的所有建筑");
        }
        int amount = space.price() / 2;
        property.setMortgaged(true);
        room.player(playerId).setCash(room.player(playerId).getCash() + amount);
        room.addEvent("MORTGAGE", playerId,
                room.player(playerId).getNickname() + " 抵押 " + space.name() + "，获得 " + amount + " 旅行币", amount);
    }

    public void unmortgageProperty(RoomGame room, String playerId, int propertyIndex) {
        assertPlaying(room);
        assertCurrentForManagement(room, playerId);
        BoardSpace space = Board.space(propertyIndex);
        PropertyState property = room.property(propertyIndex);
        PlayerState player = room.player(playerId);
        if (!playerId.equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "你不是该地产的拥有者");
        if (!property.isMortgaged()) throw new GameException("NOT_MORTGAGED", "该地产尚未抵押");
        int cost = (int) Math.ceil(space.price() * 1.1);
        if (player.getCash() < cost) throw new GameException("INSUFFICIENT_CASH", "旅行币不足");
        player.setCash(player.getCash() - cost);
        property.setMortgaged(false);
        room.addEvent("UNMORTGAGE", playerId,
                player.getNickname() + " 赎回 " + space.name() + "，支付 " + cost + " 旅行币", cost);
    }

    public void proposeTrade(RoomGame room, String fromPlayerId, String toPlayerId,
                             List<Integer> offeredProperties, List<Integer> requestedProperties,
                             int offeredCash, int requestedCash, int offeredJailCards, int requestedJailCards) {
        assertPlaying(room);
        assertTurn(room, fromPlayerId);
        GameState game = room.getGameState();
        if (game.getTrade() != null || game.getAuction() != null || game.getDebt() != null) {
            throw new GameException("ACTION_PENDING", "当前已有待处理的拍卖、交易或债务");
        }
        if (game.getTurnStep() != TurnStep.WAIT_ROLL && game.getTurnStep() != TurnStep.WAIT_END_TURN) {
            throw new GameException("INVALID_STEP", "当前不能发起交易");
        }
        PlayerState from = room.player(fromPlayerId);
        PlayerState to = room.player(toPlayerId);
        if (fromPlayerId.equals(toPlayerId) || to.isBankrupt()) throw new GameException("INVALID_TARGET", "交易对象无效");
        List<Integer> offered = safeList(offeredProperties);
        List<Integer> requested = safeList(requestedProperties);
        validateTradeAssets(room, from, to, offered, requested, offeredCash, requestedCash, offeredJailCards, requestedJailCards);
        TradeOffer trade = new TradeOffer();
        trade.setId(java.util.UUID.randomUUID().toString());
        trade.setFromPlayerId(fromPlayerId);
        trade.setToPlayerId(toPlayerId);
        trade.setOfferedPropertyIndexes(offered);
        trade.setRequestedPropertyIndexes(requested);
        trade.setOfferedCash(offeredCash);
        trade.setRequestedCash(requestedCash);
        trade.setOfferedJailCards(offeredJailCards);
        trade.setRequestedJailCards(requestedJailCards);
        trade.setResumeTurnStep(game.getTurnStep());
        game.setTrade(trade);
        game.setTurnStep(TurnStep.TRADE);
        room.addEvent("TRADE_PROPOSED", fromPlayerId,
                from.getNickname() + " 向 " + to.getNickname() + " 发起交易", null);
    }

    public void respondTrade(RoomGame room, String playerId, boolean accept) {
        assertPlaying(room);
        GameState game = room.getGameState();
        TradeOffer trade = game.getTrade();
        if (trade == null || game.getTurnStep() != TurnStep.TRADE) throw new GameException("NO_TRADE", "当前没有待处理交易");
        if (!playerId.equals(trade.getToPlayerId())) throw new GameException("NOT_TRADE_TARGET", "只有交易对象可以回应");
        if (accept) {
            PlayerState from = room.player(trade.getFromPlayerId());
            PlayerState to = room.player(trade.getToPlayerId());
            validateTradeStillValid(room, trade);
            transferAssets(room, from, to, trade.getOfferedPropertyIndexes(),
                    trade.getOfferedCash(), trade.getOfferedJailCards());
            transferAssets(room, to, from, trade.getRequestedPropertyIndexes(),
                    trade.getRequestedCash(), trade.getRequestedJailCards());
            room.addEvent("TRADE_ACCEPTED", playerId,
                    to.getNickname() + " 接受了 " + from.getNickname() + " 的交易", null);
        } else {
            room.addEvent("TRADE_REJECTED", playerId, room.player(playerId).getNickname() + " 拒绝了交易", null);
        }
        TurnStep resume = trade.getResumeTurnStep();
        game.setTrade(null);
        game.setTurnStep(resume);
    }

    public void cancelTrade(RoomGame room, String playerId) {
        GameState game = room.getGameState();
        TradeOffer trade = game.getTrade();
        if (trade == null) throw new GameException("NO_TRADE", "当前没有待处理交易");
        if (!playerId.equals(trade.getFromPlayerId())) throw new GameException("NOT_TRADE_OWNER", "只有发起者可以取消交易");
        TurnStep resume = trade.getResumeTurnStep();
        game.setTrade(null);
        game.setTurnStep(resume);
        room.addEvent("TRADE_CANCELLED", playerId, room.player(playerId).getNickname() + " 取消了交易", null);
    }

    private void transferAssets(RoomGame room, PlayerState from, PlayerState to,
                                List<Integer> properties, int cash, int jailCards) {
        from.setCash(from.getCash() - cash);
        to.setCash(to.getCash() + cash);
        if (jailCards > 0) {
            from.setGetOutOfJailCards(from.getGetOutOfJailCards() - jailCards);
            to.setGetOutOfJailCards(to.getGetOutOfJailCards() + jailCards);
        }
        properties.forEach(index -> room.property(index).setOwnerId(to.getId()));
    }

    public void settleDebt(RoomGame room, String playerId) {
        assertPlaying(room);
        GameState game = room.getGameState();
        DebtState debt = game.getDebt();
        if (debt == null || game.getTurnStep() != TurnStep.DEBT_RESOLUTION) throw new GameException("NO_DEBT", "当前没有待结算债务");
        if (!playerId.equals(debt.getDebtorId())) throw new GameException("NOT_DEBTOR", "只有债务人可以结算");
        PlayerState debtor = room.player(debt.getDebtorId());
        if (debtor.getCash() < debt.getAmount()) throw new GameException("INSUFFICIENT_CASH", "旅行币仍不足以偿还债务");
        debtor.setCash(debtor.getCash() - debt.getAmount());
        if (debt.getCreditorId() != null) {
            room.player(debt.getCreditorId()).setCash(room.player(debt.getCreditorId()).getCash() + debt.getAmount());
        }
        room.addEvent("DEBT_SETTLED", playerId,
                debtor.getNickname() + " 结算了 " + debt.getAmount() + " 旅行币债务", debt.getAmount());
        String reason = debt.getReason();
        game.setDebt(null);
        if ("监狱保释".equals(reason)) {
            debtor.setInJail(false);
            debtor.setJailTurns(0);
            game.setTurnStep(TurnStep.WAIT_ROLL);
        } else {
            game.setTurnStep(TurnStep.WAIT_END_TURN);
        }
    }

    public void declareBankruptcy(RoomGame room, String playerId) {
        assertPlaying(room);
        GameState game = room.getGameState();
        if (game.getDebt() == null || !playerId.equals(game.getDebt().getDebtorId())) {
            throw new GameException("NO_DEBT", "只有债务人可以在资金不足时宣布破产");
        }
        PlayerState debtor = room.player(playerId);
        PlayerState creditor = game.getDebt().getCreditorId() == null ? null : room.player(game.getDebt().getCreditorId());
        bankrupt(room, debtor, creditor, game.getDebt().getReason());
    }

    public void forfeit(RoomGame room, String playerId) {
        GameState game = room.getGameState();
        PlayerState player = room.player(playerId);
        if (game.getPhase() != GamePhase.PLAYING && game.getPhase() != GamePhase.PAUSED) {
            throw new GameException("INVALID_PHASE", "当前不能退出对局");
        }
        bankrupt(room, player, null, "主动退出");
    }

    private void bankrupt(RoomGame room, PlayerState debtor, PlayerState creditor, String reason) {
        GameState game = room.getGameState();
        List<Integer> owned = new ArrayList<>(room.propertyIndexesOwnedBy(debtor.getId()));
        if (creditor != null) {
            creditor.setCash(creditor.getCash() + debtor.getCash());
            for (int index : owned) {
                PropertyState property = room.property(index);
                property.setOwnerId(creditor.getId());
                property.setHouses(0);
                property.setMortgaged(false);
            }
            creditor.setGetOutOfJailCards(creditor.getGetOutOfJailCards() + debtor.getGetOutOfJailCards());
        } else {
            for (int index : owned) {
                PropertyState property = room.property(index);
                property.setOwnerId(null);
                property.setHouses(0);
                property.setMortgaged(false);
                game.getPendingAuctionIndexes().add(index);
            }
        }
        debtor.setCash(0);
        debtor.setGetOutOfJailCards(0);
        debtor.setBankrupt(true);
        debtor.setInJail(false);
        room.addEvent("BANKRUPT", debtor.getId(),
                debtor.getNickname() + " 宣告破产（" + reason + "）", null);
        game.setDebt(null);
        game.setTrade(null);
        game.setAuction(null);
        game.setPendingPurchaseIndex(null);
        boolean wasCurrentTurn = debtor.getId().equals(game.getTurnPlayerId());
        if (finishIfWon(room)) return;
        if (wasCurrentTurn) {
            if (!game.getPendingAuctionIndexes().isEmpty()) startNextQueuedAuction(room);
            else advanceTurn(room);
        } else {
            game.setTurnStep(TurnStep.WAIT_END_TURN);
        }
    }

    private void startQueuedAuctionOrEnd(RoomGame room) {
        GameState game = room.getGameState();
        if (!game.getPendingAuctionIndexes().isEmpty()) {
            startNextQueuedAuction(room);
            return;
        }
        PlayerState current = room.findPlayer(game.getTurnPlayerId()).orElse(null);
        if (current == null || current.isBankrupt()) advanceTurn(room);
        else game.setTurnStep(TurnStep.WAIT_END_TURN);
    }

    private void startNextQueuedAuction(RoomGame room) {
        GameState game = room.getGameState();
        int index = game.getPendingAuctionIndexes().remove(0);
        startAuction(room, index);
    }

    public void handleTimeout(RoomGame room) {
        GameState game = room.getGameState();
        if (game.getPhase() != GamePhase.PLAYING) return;
        TurnStep step = game.getTurnStep();
        if (step == TurnStep.WAIT_ROLL) rollDice(room, game.getTurnPlayerId());
        else if (step == TurnStep.WAIT_JAIL_DECISION) jailAction(room, game.getTurnPlayerId(), "ROLL");
        else if (step == TurnStep.WAIT_PURCHASE) declinePurchase(room, game.getTurnPlayerId());
        else if (step == TurnStep.AUCTION && game.getAuction() != null) passAuction(room, game.getAuction().getCurrentBidderId());
        else if (step == TurnStep.TRADE && game.getTrade() != null) {
            if (game.getTurnPlayerId().equals(game.getTrade().getFromPlayerId())) cancelTrade(room, game.getTrade().getFromPlayerId());
            else respondTrade(room, game.getTrade().getToPlayerId(), false);
        } else if (step == TurnStep.DEBT_RESOLUTION && game.getDebt() != null) {
            PlayerState debtor = room.player(game.getDebt().getDebtorId());
            if (debtor.getCash() >= game.getDebt().getAmount()) settleDebt(room, debtor.getId());
            else declareBankruptcy(room, debtor.getId());
        } else if (step == TurnStep.WAIT_END_TURN) endTurn(room, game.getTurnPlayerId());
    }
    private void validateTradeAssets(RoomGame room, PlayerState from, PlayerState to,
                                     List<Integer> offered, List<Integer> requested,
                                     int offeredCash, int requestedCash, int offeredJailCards, int requestedJailCards) {
        if (offered.stream().distinct().count() != offered.size() || requested.stream().distinct().count() != requested.size()) {
            throw new GameException("DUPLICATE_PROPERTY", "交易地产不能重复");
        }
        if (offeredCash < 0 || requestedCash < 0 || offeredJailCards < 0 || requestedJailCards < 0) {
            throw new GameException("INVALID_TRADE", "交易数值不能为负数");
        }
        if (offeredCash > from.getCash() || requestedCash > to.getCash()) throw new GameException("INSUFFICIENT_CASH", "交易现金超过持有金额");
        if (offeredJailCards > from.getGetOutOfJailCards() || requestedJailCards > to.getGetOutOfJailCards()) {
            throw new GameException("INSUFFICIENT_JAIL_CARDS", "出狱卡数量不足");
        }
        for (int index : offered) {
            PropertyState property = room.property(index);
            if (!from.getId().equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "你不能交易未拥有的地产");
            if (property.getHouses() > 0) throw new GameException("PROPERTY_HAS_BUILDINGS", "带建筑的地产不能交易");
        }
        for (int index : requested) {
            PropertyState property = room.property(index);
            if (!to.getId().equals(property.getOwnerId())) throw new GameException("NOT_OWNER", "对方不拥有请求的地产");
            if (property.getHouses() > 0) throw new GameException("PROPERTY_HAS_BUILDINGS", "带建筑的地产不能交易");
        }
    }

    private void validateTradeStillValid(RoomGame room, TradeOffer trade) {
        PlayerState from = room.player(trade.getFromPlayerId());
        PlayerState to = room.player(trade.getToPlayerId());
        validateTradeAssets(room, from, to, trade.getOfferedPropertyIndexes(), trade.getRequestedPropertyIndexes(),
                trade.getOfferedCash(), trade.getRequestedCash(), trade.getOfferedJailCards(), trade.getRequestedJailCards());
    }

    private List<Integer> safeList(List<Integer> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private boolean hasMortgagedInGroup(RoomGame room, int propertyIndex) {
        String group = Board.space(propertyIndex).colorGroup();
        return Board.colorGroup(group).stream().map(space -> room.property(space.index())).anyMatch(PropertyState::isMortgaged);
    }

    private boolean canBuildEvenly(RoomGame room, BoardSpace space, PropertyState property) {
        int minimum = Board.colorGroup(space.colorGroup()).stream()
                .map(item -> room.property(item.index()).getHouses()).min(Integer::compareTo).orElse(0);
        return property.getHouses() == minimum;
    }

    private boolean canSellEvenly(RoomGame room, BoardSpace space, PropertyState property) {
        int maximum = Board.colorGroup(space.colorGroup()).stream()
                .map(item -> room.property(item.index()).getHouses()).max(Integer::compareTo).orElse(0);
        return property.getHouses() == maximum;
    }

    private String nextPlayerId(RoomGame room, String anchorId, List<String> candidates) {
        if (candidates.isEmpty()) throw new GameException("NO_PLAYER", "没有可用玩家");
        List<PlayerState> ordered = room.getPlayers();
        int anchor = 0;
        for (int i = 0; i < ordered.size(); i++) if (ordered.get(i).getId().equals(anchorId)) anchor = i;
        for (int offset = 1; offset <= ordered.size(); offset++) {
            String candidate = ordered.get((anchor + offset) % ordered.size()).getId();
            if (candidates.contains(candidate)) return candidate;
        }
        return candidates.get(0);
    }

    private void advanceTurn(RoomGame room) {
        if (finishIfWon(room)) return;
        GameState game = room.getGameState();
        var active = room.activePlayers().stream().map(PlayerState::getId).toList();
        String nextId = nextPlayerId(room, game.getTurnPlayerId(), active);
        PlayerState next = room.player(nextId);
        game.setTurnPlayerId(nextId);
        game.setDoublesCount(0);
        game.setLastRollWasDoubles(false);
        game.setPendingPurchaseIndex(null);
        game.setTurnStep(next.isInJail() ? TurnStep.WAIT_JAIL_DECISION : TurnStep.WAIT_ROLL);
        room.addEvent("TURN_STARTED", nextId, "轮到 " + next.getNickname() + " 行动", null);
    }

    private boolean finishIfWon(RoomGame room) {
        List<PlayerState> active = room.activePlayers();
        if (active.size() > 1) return false;
        GameState game = room.getGameState();
        game.setPhase(GamePhase.FINISHED);
        game.setTurnStep(TurnStep.FINISHED);
        game.setWinnerId(active.isEmpty() ? null : active.get(0).getId());
        game.setDebt(null);
        game.setTrade(null);
        game.setAuction(null);
        game.setPendingPurchaseIndex(null);
        game.setVersion(game.getVersion() + 1);
        if (!active.isEmpty()) room.addEvent("GAME_FINISHED", active.get(0).getId(),
                active.get(0).getNickname() + " 赢得了城市环游记", null);
        else room.addEvent("GAME_FINISHED", null, "所有玩家均已破产", null);
        return true;
    }

    private void assertPlaying(RoomGame room) {
        if (room.getGameState().getPhase() != GamePhase.PLAYING) {
            throw new GameException("GAME_PAUSED", "对局当前已暂停或结束");
        }
    }

    private void assertTurn(RoomGame room, String playerId) {
        if (!playerId.equals(room.getGameState().getTurnPlayerId())) throw new GameException("NOT_YOUR_TURN", "还没有轮到你");
    }

    private void assertCurrentForManagement(RoomGame room, String playerId) {
        assertTurn(room, playerId);
        TurnStep step = room.getGameState().getTurnStep();
        if (step != TurnStep.WAIT_ROLL && step != TurnStep.WAIT_END_TURN && step != TurnStep.DEBT_RESOLUTION) {
            throw new GameException("INVALID_STEP", "当前不能管理资产");
        }
    }
}
