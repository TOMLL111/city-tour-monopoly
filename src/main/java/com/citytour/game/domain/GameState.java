package com.citytour.game.domain;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private GamePhase phase = GamePhase.LOBBY;
    private TurnStep turnStep = TurnStep.WAIT_ROLL;
    private GameSettings settings = GameSettings.defaults();
    private String turnPlayerId;
    private int version;
    private int diceOne;
    private int diceTwo;
    private int doublesCount;
    private boolean lastRollWasDoubles;
    private Integer pendingPurchaseIndex;
    private AuctionState auction;
    private TradeOffer trade;
    private DebtState debt;
    private String lastCardTitle;
    private String lastCardText;
    private String winnerId;
    private List<Integer> pendingAuctionIndexes = new ArrayList<>();
    private Instant turnDeadline;

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }
    public TurnStep getTurnStep() { return turnStep; }
    public void setTurnStep(TurnStep turnStep) { this.turnStep = turnStep; }
    public GameSettings getSettings() { return settings; }
    public void setSettings(GameSettings settings) { this.settings = settings; }
    public String getTurnPlayerId() { return turnPlayerId; }
    public void setTurnPlayerId(String turnPlayerId) { this.turnPlayerId = turnPlayerId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public int getDiceOne() { return diceOne; }
    public void setDiceOne(int diceOne) { this.diceOne = diceOne; }
    public int getDiceTwo() { return diceTwo; }
    public void setDiceTwo(int diceTwo) { this.diceTwo = diceTwo; }
    public int getDoublesCount() { return doublesCount; }
    public void setDoublesCount(int doublesCount) { this.doublesCount = doublesCount; }
    public boolean isLastRollWasDoubles() { return lastRollWasDoubles; }
    public void setLastRollWasDoubles(boolean lastRollWasDoubles) { this.lastRollWasDoubles = lastRollWasDoubles; }
    public Integer getPendingPurchaseIndex() { return pendingPurchaseIndex; }
    public void setPendingPurchaseIndex(Integer pendingPurchaseIndex) { this.pendingPurchaseIndex = pendingPurchaseIndex; }
    public AuctionState getAuction() { return auction; }
    public void setAuction(AuctionState auction) { this.auction = auction; }
    public TradeOffer getTrade() { return trade; }
    public void setTrade(TradeOffer trade) { this.trade = trade; }
    public DebtState getDebt() { return debt; }
    public void setDebt(DebtState debt) { this.debt = debt; }
    public String getLastCardTitle() { return lastCardTitle; }
    public void setLastCardTitle(String lastCardTitle) { this.lastCardTitle = lastCardTitle; }
    public String getLastCardText() { return lastCardText; }
    public void setLastCardText(String lastCardText) { this.lastCardText = lastCardText; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public Instant getTurnDeadline() { return turnDeadline; }
    public void setTurnDeadline(Instant turnDeadline) { this.turnDeadline = turnDeadline; }
    public List<Integer> getPendingAuctionIndexes() { return pendingAuctionIndexes; }
    public void setPendingAuctionIndexes(List<Integer> pendingAuctionIndexes) { this.pendingAuctionIndexes = pendingAuctionIndexes; }
}
