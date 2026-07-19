package com.lexho.user_service.chat.controller;

import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.dto.ChatReadReceiptResponse;
import com.lexho.user_service.chat.dto.ChatRoomPageResponse;
import com.lexho.user_service.chat.dto.ChatRoomResponse;
import com.lexho.user_service.chat.dto.CreateChatRoomRequest;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/room")
    public ApiResponse<ChatRoomResponse> createRoom(@Valid @RequestBody CreateChatRoomRequest request) {
        return ApiResponse.success("Chat room ready", chatService.createRoom(request));
    }

    @GetMapping("/rooms")
    public ApiResponse<ChatRoomPageResponse> rooms(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Chat rooms fetched", chatService.listRooms(page, size));
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatMessagePageResponse> messages(@PathVariable Long roomId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Chat messages fetched", chatService.listMessages(roomId, page, size));
    }

    @PatchMapping("/message/{id}/read")
    public ApiResponse<ChatReadReceiptResponse> markRead(@PathVariable Long id) {
        return ApiResponse.success("Chat message marked as read", chatService.markMessageRead(id));
    }
}
