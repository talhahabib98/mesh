package com.mesh.room;

import com.mesh.auth.CurrentUserResolver;
import com.mesh.room.dto.CreateRoomRequest;
import com.mesh.room.dto.RoomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request,
                                                    Authentication authentication) {
        var creator = currentUserResolver.resolve(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request, creator.getId()));
    }

    @GetMapping
    public List<RoomResponse> listRooms() {
        return roomService.listRooms();
    }
}
