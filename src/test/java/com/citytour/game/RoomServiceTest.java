package com.citytour.game;

import com.citytour.game.domain.GameEngine;
import com.citytour.game.domain.GameException;
import com.citytour.game.room.RoomCredentials;
import com.citytour.game.room.RoomService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceTest {
    @Test
    void createsAndFillsRoomToSevenPlayers() {
        RoomService service = new RoomService(new GameEngine());
        RoomCredentials host = service.createRoom("房主");
        assertThat(host.roomCode()).hasSize(6);
        for (int i = 2; i <= 7; i++) service.joinRoom(host.roomCode(), "玩家" + i);
        assertThat(service.snapshot(host.roomCode()).players()).hasSize(7);
        assertThatThrownBy(() -> service.joinRoom(host.roomCode(), "第八人"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("人数已满");
    }

    @Test
    void rejectsDuplicateNicknameInSameRoom() {
        RoomService service = new RoomService(new GameEngine());
        RoomCredentials host = service.createRoom("同名");
        assertThatThrownBy(() -> service.joinRoom(host.roomCode(), "同名"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("同名");
    }
}