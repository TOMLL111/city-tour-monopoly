package com.citytour.game.room;

import java.time.Instant;

public record ChatMessage(
        long sequence,
        Instant timestamp,
        String playerId,
        String playerName,
        String type,
        String text
) {
}
