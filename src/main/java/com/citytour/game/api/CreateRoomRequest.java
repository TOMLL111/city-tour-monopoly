package com.citytour.game.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(@NotBlank @Size(max = 16) String nickname) {
}
