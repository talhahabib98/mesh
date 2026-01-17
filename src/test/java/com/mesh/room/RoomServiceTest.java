package com.mesh.room;

import com.mesh.room.dto.CreateRoomRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    RoomRepository roomRepository;

    RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository);
    }

    @Test
    void createRoomPersistsAndReturnsResponse() {
        UUID creatorId = UUID.randomUUID();
        when(roomRepository.existsByName("general")).thenReturn(false);
        when(roomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            room.setId(UUID.randomUUID());
            room.setCreatedAt(Instant.now());
            return room;
        });

        var response = roomService.createRoom(new CreateRoomRequest("general"), creatorId);

        assertThat(response.name()).isEqualTo("general");
        assertThat(response.createdBy()).isEqualTo(creatorId);
    }

    @Test
    void createRoomRejectsDuplicateName() {
        when(roomRepository.existsByName("general")).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(new CreateRoomRequest("general"), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    void getByIdOrThrowThrowsWhenMissing() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> roomService.getByIdOrThrow(roomId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("room not found");
    }
}
