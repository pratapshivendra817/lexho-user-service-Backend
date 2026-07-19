package com.lexho.user_service.chat;

import com.lexho.user_service.chat.dto.ChatDeliveryReceiptResponse;
import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.dto.ChatMessageResponse;
import com.lexho.user_service.chat.dto.ChatParticipantSummary;
import com.lexho.user_service.chat.dto.ChatReadReceiptResponse;
import com.lexho.user_service.chat.dto.ChatRoomPageResponse;
import com.lexho.user_service.chat.dto.ChatRoomResponse;
import com.lexho.user_service.chat.dto.ChatSendMessageRequest;
import com.lexho.user_service.chat.dto.ChatSocketEvent;
import com.lexho.user_service.chat.dto.ChatTypingEventResponse;
import com.lexho.user_service.chat.dto.ChatTypingRequest;
import com.lexho.user_service.chat.dto.CreateChatRoomRequest;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatDtoCoverageAdditionalTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void chatRequestDtosValidateRequiredFields() {
        ChatSendMessageRequest send = new ChatSendMessageRequest();
        ChatTypingRequest typing = new ChatTypingRequest();
        CreateChatRoomRequest create = new CreateChatRoomRequest();

        Set<ConstraintViolation<ChatSendMessageRequest>> sendViolations = validator.validate(send);
        assertThat(sendViolations).extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("roomId", "content");
        assertThat(send.getMessageType()).isEqualTo(ChatMessageType.TEXT);

        assertThat(validator.validate(typing)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("roomId");
        assertThat(validator.validate(create)).extracting(v -> v.getPropertyPath().toString())
                .containsExactly("orderId");

        send.setRoomId(1L);
        send.setContent("hello");
        send.setMessageType(ChatMessageType.TEXT);
        typing.setRoomId(2L);
        create.setOrderId(3L);

        assertThat(validator.validate(send)).isEmpty();
        assertThat(validator.validate(typing)).isEmpty();
        assertThat(validator.validate(create)).isEmpty();
    }

    @Test
    void chatResponseDtosExposeBuildersAndAccessors() {
        Instant now = Instant.parse("2026-07-17T08:00:00Z");
        ChatParticipantSummary participant = new ChatParticipantSummary(ChatParticipantType.CUSTOMER, 11L);
        ChatMessageResponse message = ChatMessageResponse.builder()
                .messageId(1L)
                .roomId(2L)
                .senderType(ChatParticipantType.PARTNER)
                .senderId(3L)
                .messageType(ChatMessageType.TEXT)
                .content("Hi")
                .deliveryStatus(ChatMessageDeliveryStatus.DELIVERED)
                .deliveredAt(now)
                .readAt(now)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        ChatMessagePageResponse messagePage = ChatMessagePageResponse.builder()
                .content(List.of(message))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .unreadCount(0L)
                .build();
        ChatRoomResponse room = ChatRoomResponse.builder()
                .roomId(4L)
                .orderId(5L)
                .lastMessagePreview("Hi")
                .lastMessageAt(now)
                .unreadCount(2L)
                .participants(List.of(participant))
                .createdAt(now)
                .updatedAt(now)
                .build();
        ChatRoomPageResponse roomPage = ChatRoomPageResponse.builder()
                .content(List.of(room))
                .page(1)
                .size(10)
                .totalElements(3L)
                .totalPages(1)
                .build();
        ChatDeliveryReceiptResponse delivery = ChatDeliveryReceiptResponse.builder()
                .roomId(6L)
                .messageId(7L)
                .deliveredAt(now)
                .build();
        ChatReadReceiptResponse read = ChatReadReceiptResponse.builder()
                .roomId(8L)
                .messageId(9L)
                .readAt(now)
                .build();
        ChatTypingEventResponse typing = ChatTypingEventResponse.builder()
                .roomId(10L)
                .participantType(ChatParticipantType.CUSTOMER)
                .participantId(12L)
                .occurredAt(now)
                .build();
        ChatSocketEvent<String> socketEvent = ChatSocketEvent.<String>builder()
                .eventType(ChatEventType.CHAT_MESSAGE)
                .roomId(13L)
                .payload("payload")
                .occurredAt(now)
                .build();

        assertThat(participant.getParticipantType()).isEqualTo(ChatParticipantType.CUSTOMER);
        assertThat(messagePage.getContent()).singleElement().extracting(ChatMessageResponse::getContent).isEqualTo("Hi");
        assertThat(roomPage.getContent()).singleElement().extracting(ChatRoomResponse::getLastMessagePreview).isEqualTo("Hi");
        assertThat(delivery.getMessageId()).isEqualTo(7L);
        assertThat(read.getReadAt()).isEqualTo(now);
        assertThat(typing.getParticipantId()).isEqualTo(12L);
        assertThat(socketEvent.getPayload()).isEqualTo("payload");
        assertThat(socketEvent.getEventType()).isEqualTo(ChatEventType.CHAT_MESSAGE);
    }
}
