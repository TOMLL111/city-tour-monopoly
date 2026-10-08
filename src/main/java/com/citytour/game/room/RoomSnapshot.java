package com.citytour.game.room;

import com.citytour.game.domain.BoardSpace;
import com.citytour.game.domain.GameState;
import com.citytour.game.domain.PlayerState;
import com.citytour.game.domain.PropertyState;

import java.util.List;
import java.util.Map;

public record RoomSnapshot(
        String roomCode,
        String hostId,
        int version,
        List<PlayerState> players,
        List<BoardSpace> board,
        Map<Integer, PropertyState> properties,
        GameState gameState,
        List<RoomEvent> events,
        List<ChatMessage> chatMessages
) {
}
