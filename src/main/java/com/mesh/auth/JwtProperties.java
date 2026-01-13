package com.mesh.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mesh.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
