package com.mesh.realtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesh.node")
public record NodeProperties(String id) {
}
