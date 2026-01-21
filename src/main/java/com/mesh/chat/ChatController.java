package com.mesh.chat;

import com.mesh.auth.CurrentUserResolver;
import com.mesh.chat.dto.SendMessageRequest;
import com.mesh.message.MessageService;
import com.mesh.realtime.ChatMessagePublisher;
import com.mesh.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final CurrentUserResolver currentUserResolver;
    private final ChatMessagePublisher chatMessagePublisher;

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal) {
        User sender = currentUserResolver.resolve(principal.getName());

        var saved = messageService.save(request.roomId(), sender.getId(), sender.getUsername(), request.content());

        // Published to Redis rather than broadcast directly: every instance (including this
        // one) delivers to its own locally-connected WebSocket sessions via ChatMessageSubscriber,
        // so clients stay in sync regardless of which node they're connected to.
        chatMessagePublisher.publish(saved);
    }
}
