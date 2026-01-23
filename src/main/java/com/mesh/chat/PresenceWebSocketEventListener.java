package com.mesh.chat;

import com.mesh.realtime.PresencePublisher;
import com.mesh.realtime.PresenceService;
import com.mesh.realtime.dto.PresenceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks which chat room each WebSocket session is subscribed to, purely so that a
 * disconnect can be translated into a "left" presence event for the right room(s) -
 * STOMP disconnects don't carry the destination, only the session id.
 */
@Component
@RequiredArgsConstructor
public class PresenceWebSocketEventListener {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms/([0-9a-fA-F-]{36})$");

    private final PresenceService presenceService;
    private final PresencePublisher presencePublisher;

    private final ConcurrentHashMap<String, Set<RoomMembership>> sessionMemberships = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = ROOM_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            return;
        }

        UUID roomId = UUID.fromString(matcher.group(1));
        String username = principal.getName();

        sessionMemberships
                .computeIfAbsent(accessor.getSessionId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(new RoomMembership(roomId, username));

        presenceService.join(roomId, username);
        presencePublisher.publish(roomId, username, PresenceStatus.JOINED);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Set<RoomMembership> memberships = sessionMemberships.remove(event.getSessionId());
        if (memberships == null) {
            return;
        }

        for (RoomMembership membership : memberships) {
            presenceService.leave(membership.roomId(), membership.username());
            presencePublisher.publish(membership.roomId(), membership.username(), PresenceStatus.LEFT);
        }
    }

    private record RoomMembership(UUID roomId, String username) {
    }
}
