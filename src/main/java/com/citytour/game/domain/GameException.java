package com.citytour.game.domain;

public class GameException extends RuntimeException {
    private final String code;

    public GameException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
