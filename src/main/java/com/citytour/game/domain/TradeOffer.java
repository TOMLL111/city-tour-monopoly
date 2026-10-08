package com.citytour.game.domain;

import java.util.ArrayList;
import java.util.List;

public class TradeOffer {
    public enum Status { PENDING, ACCEPTED, REJECTED, CANCELLED }

    private String id;
    private String fromPlayerId;
    private String toPlayerId;
    private List<Integer> offeredPropertyIndexes = new ArrayList<>();
    private List<Integer> requestedPropertyIndexes = new ArrayList<>();
    private int offeredCash;
    private int requestedCash;
    private int offeredJailCards;
    private int requestedJailCards;
    private TurnStep resumeTurnStep = TurnStep.WAIT_END_TURN;
    private Status status = Status.PENDING;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFromPlayerId() { return fromPlayerId; }
    public void setFromPlayerId(String fromPlayerId) { this.fromPlayerId = fromPlayerId; }
    public String getToPlayerId() { return toPlayerId; }
    public void setToPlayerId(String toPlayerId) { this.toPlayerId = toPlayerId; }
    public List<Integer> getOfferedPropertyIndexes() { return offeredPropertyIndexes; }
    public void setOfferedPropertyIndexes(List<Integer> offeredPropertyIndexes) { this.offeredPropertyIndexes = offeredPropertyIndexes; }
    public List<Integer> getRequestedPropertyIndexes() { return requestedPropertyIndexes; }
    public void setRequestedPropertyIndexes(List<Integer> requestedPropertyIndexes) { this.requestedPropertyIndexes = requestedPropertyIndexes; }
    public int getOfferedCash() { return offeredCash; }
    public void setOfferedCash(int offeredCash) { this.offeredCash = offeredCash; }
    public int getRequestedCash() { return requestedCash; }
    public void setRequestedCash(int requestedCash) { this.requestedCash = requestedCash; }
    public int getOfferedJailCards() { return offeredJailCards; }
    public void setOfferedJailCards(int offeredJailCards) { this.offeredJailCards = offeredJailCards; }
    public int getRequestedJailCards() { return requestedJailCards; }
    public void setRequestedJailCards(int requestedJailCards) { this.requestedJailCards = requestedJailCards; }
    public TurnStep getResumeTurnStep() { return resumeTurnStep; }
    public void setResumeTurnStep(TurnStep resumeTurnStep) { this.resumeTurnStep = resumeTurnStep; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
