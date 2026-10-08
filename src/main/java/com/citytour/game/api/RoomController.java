package com.citytour.game.api;

import com.citytour.game.room.RoomCredentials;
import com.citytour.game.room.RoomService;
import com.citytour.game.room.RoomSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomCredentials createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request.nickname());
    }

    @PostMapping("/{code}/join")
    public RoomCredentials joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(code, request.nickname());
    }

    @GetMapping("/{code}")
    public RoomSnapshot snapshot(@PathVariable String code,
                                 @RequestHeader("X-Player-Token") String playerToken) {
        roomService.authenticate(code, playerToken);
        return roomService.snapshot(code);
    }
}
