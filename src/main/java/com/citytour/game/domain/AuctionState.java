package com.citytour.game.domain;

import java.util.ArrayList;
import java.util.List;

public class AuctionState {
    private int propertyIndex;
    private String currentBidderId;
    private int highBid;
    private String highBidderId;
    private List<String> activeBidderIds = new ArrayList<>();

    public AuctionState() {
    }

    public AuctionState(int propertyIndex, String firstBidderId, List<String> activeBidderIds) {
        this.propertyIndex = propertyIndex;
        this.currentBidderId = firstBidderId;
        this.activeBidderIds = new ArrayList<>(activeBidderIds);
    }

    public int getPropertyIndex() { return propertyIndex; }
    public void setPropertyIndex(int propertyIndex) { this.propertyIndex = propertyIndex; }
    public String getCurrentBidderId() { return currentBidderId; }
    public void setCurrentBidderId(String currentBidderId) { this.currentBidderId = currentBidderId; }
    public int getHighBid() { return highBid; }
    public void setHighBid(int highBid) { this.highBid = highBid; }
    public String getHighBidderId() { return highBidderId; }
    public void setHighBidderId(String highBidderId) { this.highBidderId = highBidderId; }
    public List<String> getActiveBidderIds() { return activeBidderIds; }
    public void setActiveBidderIds(List<String> activeBidderIds) { this.activeBidderIds = activeBidderIds; }
}
