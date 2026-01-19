package com.mesh.message;

import com.mesh.message.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final MessageRepository messageRepository;

    public ChatMessageResponse save(UUID roomId, UUID senderId, String senderUsername, String content) {
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(content)
                .build();

        return toResponse(messageRepository.save(message));
    }

    public List<ChatMessageResponse> getHistory(UUID roomId, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_HISTORY_LIMIT;

        List<ChatMessage> messages = messageRepository.findByRoomIdOrderBySentAtDesc(
                roomId, PageRequest.of(0, effectiveLimit));

        Collections.reverse(messages);
        return messages.stream().map(this::toResponse).toList();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderUsername(),
                message.getContent(),
                message.getSentAt());
    }
}
