package com.mesh.realtime;

import com.mesh.realtime.dto.PresenceEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PresenceSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void onMessage(Message message, @Nullable byte[] pattern) {
        PresenceEvent event = objectMapper.readValue(message.getBody(), PresenceEvent.class);
        messagingTemplate.convertAndSend("/topic/rooms/" + event.roomId() + "/presence", event);
    }
}
