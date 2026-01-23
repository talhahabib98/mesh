package com.mesh.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final RedisChannelProperties channelProperties;

    public void join(UUID roomId, String username) {
        redisTemplate.opsForSet().add(key(roomId), username);
    }

    public void leave(UUID roomId, String username) {
        redisTemplate.opsForSet().remove(key(roomId), username);
    }

    public Set<String> onlineUsers(UUID roomId) {
        Set<String> members = redisTemplate.opsForSet().members(key(roomId));
        return members != null ? members : Set.of();
    }

    private String key(UUID roomId) {
        return channelProperties.presenceKeyPrefix() + roomId;
    }
}
