package com.mesh.room.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(UUID id, String name, UUID createdBy, Instant createdAt) {
}
