package com.citytour.game;

import com.citytour.game.domain.Board;
import com.citytour.game.domain.GameEngine;
import com.citytour.game.domain.GameException;
import com.citytour.game.domain.GamePhase;
import com.citytour.game.domain.PlayerState;
import com.citytour.game.domain.PropertyState;
import com.citytour.game.domain.TurnStep;
import com.citytour.game.room.RoomGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameEngineTest {
    private GameEngine engine;
    private RoomGame room;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        room = new RoomGame("ABC123", new PlayerState("p1", "甲", "t1", true));
        room.getPlayers().add(new PlayerState("p2", "乙", "t2", false));
        room.getPlayers().add(new PlayerState("p3", "丙", "t3", false));
        room.getPlayers().forEach(player -> player.setReady(true));
        engine.startGame(room);
    }

    @Test
    void startsThreePlayerGame() {
        assertThat(room.getGameState().getPhase()).isEqualTo(GamePhase.PLAYING);
        assertThat(room.getGameState().getTurnPlayerId()).isEqualTo("p1");
        assertThat(room.getGameState().getTurnStep()).isEqualTo(TurnStep.WAIT_ROLL);
    }

    @Test
    void buysPropertyAndDeductsCash() {
        room.getGameState().setTurnStep(TurnStep.WAIT_PURCHASE);
        room.getGameState().setPendingPurchaseIndex(1);
        engine.buyProperty(room, "p1");
        assertThat(room.property(1).getOwnerId()).isEqualTo("p1");
        assertThat(room.player("p1").getCash()).isEqualTo(1440);
    }

    @Test
    void doublesRentWhenWholeGroupIsOwned() {
        room.property(1).setOwnerId("p2");
        room.property(3).setOwnerId("p2");
        int rent = engine.calculateRent(room, Board.space(1), room.property(1));
        assertThat(rent).isEqualTo(4);
    }

    @Test
    void paysToLeaveJail() {
        PlayerState player = room.player("p1");
        player.setInJail(true);
        room.getGameState().setTurnStep(TurnStep.WAIT_JAIL_DECISION);
        engine.jailAction(room, "p1", "PAY");
        assertThat(player.isInJail()).isFalse();
        assertThat(player.getCash()).isEqualTo(1450);
        assertThat(room.getGameState().getTurnStep()).isEqualTo(TurnStep.WAIT_ROLL);
    }

    @Test
    void acceptsAPropertyAndCashTrade() {
        PropertyState property = room.property(1);
        property.setOwnerId("p1");
        engine.proposeTrade(room, "p1", "p2", List.of(1), List.of(), 0, 100, 0, 0);
        engine.respondTrade(room, "p2", true);
        assertThat(property.getOwnerId()).isEqualTo("p2");
        assertThat(room.player("p1").getCash()).isEqualTo(1600);
        assertThat(room.player("p2").getCash()).isEqualTo(1400);
    }

    @Test
    void rejectsInvalidTurnActions() {
        assertThatThrownBy(() -> engine.rollDice(room, "p2"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("轮到");
    }
}