package com.mesh.message;

import com.mesh.message.dto.ChatMessageResponse;
import com.mesh.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final RoomService roomService;

    @GetMapping
    public List<ChatMessageResponse> history(@PathVariable UUID roomId,
                                              @RequestParam(defaultValue = "50") int limit) {
        roomService.getByIdOrThrow(roomId);
        return messageService.getHistory(roomId, limit);
    }
}
