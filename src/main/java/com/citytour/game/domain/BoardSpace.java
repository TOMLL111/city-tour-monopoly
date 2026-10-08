package com.citytour.game.domain;

import java.util.List;

public record BoardSpace(
        int index,
        String name,
        SpaceType type,
        String colorGroup,
        String colorHex,
        int price,
        int houseCost,
        List<Integer> rentTable,
        int taxAmount,
        String description
) {
    public boolean isPurchasable() {
        return type == SpaceType.PROPERTY || type == SpaceType.RAILROAD || type == SpaceType.UTILITY;
    }
}
