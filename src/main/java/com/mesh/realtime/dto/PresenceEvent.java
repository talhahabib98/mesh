package com.mesh.realtime.dto;

import java.util.UUID;

public record PresenceEvent(UUID roomId, String username, PresenceStatus status, String originNode) {
}
