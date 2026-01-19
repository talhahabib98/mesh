package com.mesh.message.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        UUID senderId,
        String senderUsername,
        String content,
        Instant sentAt
) {
}
