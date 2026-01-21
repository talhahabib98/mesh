package com.mesh.realtime;

import com.mesh.message.dto.ChatMessageResponse;
import com.mesh.realtime.dto.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final StringRedisTemplate redisTemplate;
    private final RedisChannelProperties channelProperties;
    private final NodeProperties nodeProperties;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publish(ChatMessageResponse message) {
        ChatBroadcastMessage broadcast = new ChatBroadcastMessage(message, nodeProperties.id());
        redisTemplate.convertAndSend(channelProperties.chatChannel(), objectMapper.writeValueAsString(broadcast));
    }
}
