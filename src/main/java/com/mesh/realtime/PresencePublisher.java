package com.mesh.realtime;

import com.mesh.realtime.dto.PresenceEvent;
import com.mesh.realtime.dto.PresenceStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PresencePublisher {

    private final StringRedisTemplate redisTemplate;
    private final RedisChannelProperties channelProperties;
    private final NodeProperties nodeProperties;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publish(UUID roomId, String username, PresenceStatus status) {
        PresenceEvent event = new PresenceEvent(roomId, username, status, nodeProperties.id());
        redisTemplate.convertAndSend(channelProperties.presenceChannel(), objectMapper.writeValueAsString(event));
    }
}
