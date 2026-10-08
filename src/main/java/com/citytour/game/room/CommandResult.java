package com.citytour.game.room;

public record CommandResult(RoomSnapshot snapshot, boolean duplicate) {
}
