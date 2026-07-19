package com.lexho.user_service.chat.controller;

import com.lexho.user_service.chat.dto.ChatSendMessageRequest;
import com.lexho.user_service.chat.dto.ChatTypingRequest;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload ChatSendMessageRequest request, Principal principal) {
        chatService.sendMessage(request, actor(principal));
    }

    @MessageMapping("/chat.typing.start")
    public void typingStart(@Valid @Payload ChatTypingRequest request, Principal principal) {
        chatService.publishTyping(request.getRoomId(), actor(principal), ChatEventType.TYPING_START);
    }

    @MessageMapping("/chat.typing.stop")
    public void typingStop(@Valid @Payload ChatTypingRequest request, Principal principal) {
        chatService.publishTyping(request.getRoomId(), actor(principal), ChatEventType.TYPING_STOP);
    }

    private ChatActor actor(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new IllegalArgumentException("Authenticated chat actor is required");
        }
        return ChatActor.fromAuthentication(authentication);
    }
}
