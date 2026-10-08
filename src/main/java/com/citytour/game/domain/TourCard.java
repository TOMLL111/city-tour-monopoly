package com.citytour.game.domain;

public record TourCard(
        String id,
        String title,
        String text,
        CardActionType action,
        int amount,
        int destination,
        int houseRepair,
        int hotelRepair
) {
}
