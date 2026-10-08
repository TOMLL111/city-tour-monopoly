package com.citytour.game.domain;

public class PropertyState {
    private int index;
    private String ownerId;
    private boolean mortgaged;
    private int houses;

    public PropertyState() {
    }

    public PropertyState(int index) {
        this.index = index;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public boolean isMortgaged() { return mortgaged; }
    public void setMortgaged(boolean mortgaged) { this.mortgaged = mortgaged; }
    public int getHouses() { return houses; }
    public void setHouses(int houses) { this.houses = houses; }
}
