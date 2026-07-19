package com.lexho.user_service.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.dto.ChatReadReceiptResponse;
import com.lexho.user_service.chat.dto.ChatRoomPageResponse;
import com.lexho.user_service.chat.dto.ChatRoomResponse;
import com.lexho.user_service.chat.dto.CreateChatRoomRequest;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.config.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void createRoomShouldReturnCreatedRoom() throws Exception {
        when(chatService.createRoom(any(CreateChatRoomRequest.class))).thenReturn(ChatRoomResponse.builder()
                .roomId(101L)
                .orderId(55L)
                .unreadCount(0L)
                .participants(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        CreateChatRoomRequest request = new CreateChatRoomRequest();
        request.setOrderId(55L);

        mockMvc.perform(post("/api/v1/chat/room")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roomId").value(101))
                .andExpect(jsonPath("$.data.orderId").value(55));
    }

    @Test
    void roomsShouldReturnPagedRooms() throws Exception {
        when(chatService.listRooms(0, 20)).thenReturn(ChatRoomPageResponse.builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0L)
                .totalPages(0)
                .build());

        mockMvc.perform(get("/api/v1/chat/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    void messagesShouldReturnPagedMessages() throws Exception {
        when(chatService.listMessages(101L, 0, 20)).thenReturn(ChatMessagePageResponse.builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0L)
                .totalPages(0)
                .unreadCount(3L)
                .build());

        mockMvc.perform(get("/api/v1/chat/101/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void markReadShouldReturnReceipt() throws Exception {
        when(chatService.markMessageRead(501L)).thenReturn(ChatReadReceiptResponse.builder()
                .roomId(101L)
                .messageId(501L)
                .readAt(Instant.now())
                .build());

        mockMvc.perform(patch("/api/v1/chat/message/501/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageId").value(501));
    }
}
