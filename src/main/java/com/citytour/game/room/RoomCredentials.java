package com.citytour.game.room;

public record RoomCredentials(String roomCode, String playerId, String playerToken, RoomSnapshot snapshot) {
}
