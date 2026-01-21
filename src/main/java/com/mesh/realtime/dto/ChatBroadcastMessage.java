package com.mesh.realtime.dto;

import com.mesh.message.dto.ChatMessageResponse;

public record ChatBroadcastMessage(ChatMessageResponse message, String originNode) {
}
