package com.mesh.realtime;

import com.mesh.realtime.dto.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public void onMessage(Message message, @Nullable byte[] pattern) {
        ChatBroadcastMessage broadcast = objectMapper.readValue(message.getBody(), ChatBroadcastMessage.class);
        log.debug("relaying chat message in room {} received from node {}",
                broadcast.message().roomId(), broadcast.originNode());

        messagingTemplate.convertAndSend("/topic/rooms/" + broadcast.message().roomId(), broadcast);
    }
}
