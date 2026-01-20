package com.mesh.chat;

import com.mesh.auth.CurrentUserResolver;
import com.mesh.chat.dto.SendMessageRequest;
import com.mesh.message.MessageService;
import com.mesh.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final CurrentUserResolver currentUserResolver;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest request, Principal principal) {
        User sender = currentUserResolver.resolve(principal.getName());

        var saved = messageService.save(request.roomId(), sender.getId(), sender.getUsername(), request.content());

        messagingTemplate.convertAndSend("/topic/rooms/" + request.roomId(), saved);
    }
}
