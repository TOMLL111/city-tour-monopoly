package com.citytour.game.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class PlayerState {
    private String id;
    private String nickname;
    @JsonIgnore
    private String token;
    private boolean host;
    private boolean ready;
    private boolean connected;
    private boolean bankrupt;
    private int position;
    private int cash = 1500;
    private int jailTurns;
    private boolean inJail;
    private int getOutOfJailCards;

    public PlayerState() {
    }

    public PlayerState(String id, String nickname, String token, boolean host) {
        this.id = id;
        this.nickname = nickname;
        this.token = token;
        this.host = host;
        this.connected = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public boolean isBankrupt() { return bankrupt; }
    public void setBankrupt(boolean bankrupt) { this.bankrupt = bankrupt; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getCash() { return cash; }
    public void setCash(int cash) { this.cash = Math.max(0, cash); }
    public int getJailTurns() { return jailTurns; }
    public void setJailTurns(int jailTurns) { this.jailTurns = jailTurns; }
    public boolean isInJail() { return inJail; }
    public void setInJail(boolean inJail) { this.inJail = inJail; }
    public int getGetOutOfJailCards() { return getOutOfJailCards; }
    public void setGetOutOfJailCards(int getOutOfJailCards) { this.getOutOfJailCards = getOutOfJailCards; }
}
