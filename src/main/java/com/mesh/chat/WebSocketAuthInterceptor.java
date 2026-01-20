package com.mesh.chat;

import com.mesh.auth.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    @Nullable
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
                throw new IllegalArgumentException("missing or malformed Authorization header on STOMP CONNECT");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            if (!jwtService.isValid(token)) {
                throw new IllegalArgumentException("invalid or expired token");
            }

            String username = jwtService.extractUsername(token);
            accessor.setUser(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        }

        return message;
    }
}
