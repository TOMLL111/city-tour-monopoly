package com.citytour.game.room;

import java.time.Instant;

public record RoomEvent(
        long sequence,
        Instant timestamp,
        String type,
        String actorId,
        String message,
        Integer amount
) {
}
