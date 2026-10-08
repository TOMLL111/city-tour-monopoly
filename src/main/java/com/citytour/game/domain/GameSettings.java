package com.citytour.game.domain;

public record GameSettings(int turnTimeLimitSeconds) {
    public static GameSettings defaults() {
        return new GameSettings(60);
    }
}
