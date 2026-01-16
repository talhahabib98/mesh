package com.mesh.room;

import com.mesh.room.dto.CreateRoomRequest;
import com.mesh.room.dto.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomResponse createRoom(CreateRoomRequest request, UUID creatorId) {
        if (roomRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "room name already in use");
        }

        ChatRoom room = ChatRoom.builder()
                .name(request.name())
                .createdBy(creatorId)
                .build();

        return toResponse(roomRepository.save(room));
    }

    public List<RoomResponse> listRooms() {
        return roomRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ChatRoom getByIdOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "room not found"));
    }

    private RoomResponse toResponse(ChatRoom room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCreatedBy(), room.getCreatedAt());
    }
}
