package com.mesh.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesh.redis")
public record RedisChannelProperties(String chatChannel) {
}
