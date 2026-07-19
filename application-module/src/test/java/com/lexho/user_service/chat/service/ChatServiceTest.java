package com.lexho.user_service.chat.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.chat.dto.ChatMessagePageResponse;
import com.lexho.user_service.chat.dto.ChatReadReceiptResponse;
import com.lexho.user_service.chat.dto.ChatSendMessageRequest;
import com.lexho.user_service.chat.dto.CreateChatRoomRequest;
import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import com.lexho.user_service.chat.repository.ChatMessageRepository;
import com.lexho.user_service.chat.repository.ChatParticipantRepository;
import com.lexho.user_service.chat.repository.ChatRoomRepository;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.provider.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatParticipantRepository chatParticipantRepository = mock(ChatParticipantRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final PartnerRepository partnerRepository = mock(PartnerRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final SimpUserRegistry simpUserRegistry = mock(SimpUserRegistry.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ChatService chatService = new ChatService(
            chatRoomRepository,
            chatParticipantRepository,
            chatMessageRepository,
            orderRepository,
            customerRepository,
            partnerRepository,
            securityUtils,
            redisTemplate,
            simpUserRegistry,
            messagingTemplate
    );

    @Test
    void createRoomShouldCreateOneRoomPerOrder() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        Order order = new Order();
        order.setId(55L);
        order.setUserId(11L);
        order.setAssignedPartnerId(77L);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));
        when(chatRoomRepository.findByOrderIdAndDeletedFalse(55L)).thenReturn(Optional.empty());
        when(customerRepository.existsById(11L)).thenReturn(true);
        when(partnerRepository.existsById(77L)).thenReturn(true);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", 101L);
            ReflectionTestUtils.setField(room, "createdAt", Instant.now());
            ReflectionTestUtils.setField(room, "updatedAt", Instant.now());
            return room;
        });
        when(chatParticipantRepository.save(any(ChatParticipant.class))).thenAnswer(invocation -> {
            ChatParticipant participant = invocation.getArgument(0);
            ReflectionTestUtils.setField(participant, "id", 201L);
            ReflectionTestUtils.setField(participant, "createdAt", Instant.now());
            ReflectionTestUtils.setField(participant, "updatedAt", Instant.now());
            return participant;
        });
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(Optional.of(participant(room(101L, 55L, 11L, 77L), ChatParticipantType.CUSTOMER, 11L)));

        var response = chatService.createRoom(request(55L));

        assertEquals(101L, response.getRoomId());
        assertEquals(55L, response.getOrderId());
        assertEquals(0L, response.getUnreadCount());
    }

    @Test
    void createRoomShouldReturnExistingRoomWithoutCreatingNewEntities() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        Order order = new Order();
        order.setId(55L);
        order.setUserId(11L);
        order.setAssignedPartnerId(77L);
        ChatRoom existingRoom = room(101L, 55L, 11L, 77L);
        ChatParticipant customer = participant(existingRoom, ChatParticipantType.CUSTOMER, 11L);
        customer.setUnreadCount(4L);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));
        when(chatRoomRepository.findByOrderIdAndDeletedFalse(55L)).thenReturn(Optional.of(existingRoom));
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(Optional.of(customer));

        var response = chatService.createRoom(request(55L));

        assertEquals(101L, response.getRoomId());
        assertEquals(4L, response.getUnreadCount());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(chatParticipantRepository, never()).save(any(ChatParticipant.class));
    }

    @Test
    void createRoomShouldRejectMissingPartnerOrUnknownParticipants() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));

        Order noPartnerOrder = new Order();
        noPartnerOrder.setId(55L);
        noPartnerOrder.setUserId(11L);
        noPartnerOrder.setAssignedPartnerId(null);
        when(orderRepository.findById(55L)).thenReturn(Optional.of(noPartnerOrder));
        when(chatRoomRepository.findByOrderIdAndDeletedFalse(55L)).thenReturn(Optional.empty());

        BadRequestException noPartner = assertThrows(BadRequestException.class, () -> chatService.createRoom(request(55L)));
        assertEquals("Partner is not assigned to this order", noPartner.getMessage());

        Order withPartner = new Order();
        withPartner.setId(56L);
        withPartner.setUserId(11L);
        withPartner.setAssignedPartnerId(77L);
        when(orderRepository.findById(56L)).thenReturn(Optional.of(withPartner));
        when(chatRoomRepository.findByOrderIdAndDeletedFalse(56L)).thenReturn(Optional.empty());
        when(customerRepository.existsById(11L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> chatService.createRoom(request(56L)));

        when(customerRepository.existsById(11L)).thenReturn(true);
        when(partnerRepository.existsById(77L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> chatService.createRoom(request(56L)));
    }

    @Test
    void createRoomShouldRejectUnauthorizedActorAndMissingAuthentication() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(99L));
        Order foreignOrder = new Order();
        foreignOrder.setId(57L);
        foreignOrder.setUserId(11L);
        foreignOrder.setAssignedPartnerId(77L);
        when(orderRepository.findById(57L)).thenReturn(Optional.of(foreignOrder));

        assertThrows(AccessDeniedException.class, () -> chatService.createRoom(request(57L)));

        when(securityUtils.getCurrentRole()).thenReturn(Optional.empty());
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(com.lexho.user_service.exception.UnauthorizedException.class,
                () -> chatService.listRooms(0, 20));
    }

    @Test
    void listRoomsShouldClampPageAndSizeAndReturnUnreadDefault() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        ChatRoom room = room(101L, 55L, 11L, 77L);
        when(chatRoomRepository.findVisibleRooms(eq(ChatParticipantType.CUSTOMER), eq(11L), any()))
                .thenAnswer(invocation -> {
                    org.springframework.data.domain.PageRequest pageRequest = invocation.getArgument(2);
                    assertEquals(0, pageRequest.getPageNumber());
                    assertEquals(100, pageRequest.getPageSize());
                    return new PageImpl<>(List.of(room), pageRequest, 1);
                });
        when(chatParticipantRepository.findByRoom_IdInAndParticipantTypeAndParticipantIdAndDeletedFalse(
                List.of(101L), ChatParticipantType.CUSTOMER, 11L)).thenReturn(List.of());

        var response = chatService.listRooms(-5, 999);

        assertEquals(1, response.getContent().size());
        assertEquals(0L, response.getContent().get(0).getUnreadCount());
        assertEquals(0, response.getPage());
        assertEquals(100, response.getSize());
    }

    @Test
    void listMessagesShouldMarkPendingMessagesDeliveredAndUseDefaultPageSize() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("PARTNER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(77L));
        ChatRoom room = room(101L, 55L, 11L, 77L);
        ChatParticipant partner = participant(room, ChatParticipantType.PARTNER, 77L);
        partner.setUnreadCount(2L);
        ChatMessage pending = message(room, 501L, ChatParticipantType.CUSTOMER, 11L, ChatMessageDeliveryStatus.SENT);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(true);
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(Optional.of(partner));
        when(chatMessageRepository.findByRoom_IdAndSenderTypeNotAndDeletedFalseAndDeliveryStatusIn(
                101L, ChatParticipantType.PARTNER, List.of(ChatMessageDeliveryStatus.SENT)))
                .thenReturn(List.of(pending));
        when(chatMessageRepository.findByRoom_IdAndDeletedFalse(eq(101L), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable pageable = invocation.getArgument(1);
                    assertEquals(20, pageable.getPageSize());
                    return new PageImpl<>(List.of(pending), pageable, 1);
                });

        ChatMessagePageResponse response = chatService.listMessages(101L, -1, 0);

        assertEquals(1, response.getContent().size());
        assertEquals(2L, response.getUnreadCount());
        assertEquals(ChatMessageDeliveryStatus.DELIVERED, pending.getDeliveryStatus());
        assertTrue(pending.getDeliveredAt() != null);
        verify(chatMessageRepository).saveAll(List.of(pending));
        verify(messagingTemplate).convertAndSendToUser(eq("PARTNER:77"), eq("/queue/chat"), any());
    }

    @Test
    void listMessagesByOrderIdShouldCreateOrReuseRoomAndReturnMessages() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("PARTNER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(77L));
        Order order = new Order();
        order.setId(55L);
        order.setUserId(11L);
        order.setAssignedPartnerId(77L);
        ChatRoom room = room(101L, 55L, 11L, 77L);
        ChatParticipant partner = participant(room, ChatParticipantType.PARTNER, 77L);
        ChatMessage pending = message(room, 501L, ChatParticipantType.CUSTOMER, 11L, ChatMessageDeliveryStatus.SENT);

        when(orderRepository.findById(55L)).thenReturn(Optional.of(order));
        when(chatRoomRepository.findByOrderIdAndDeletedFalse(55L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(Optional.of(partner));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(true);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.findByRoom_IdAndSenderTypeNotAndDeletedFalseAndDeliveryStatusIn(
                101L, ChatParticipantType.PARTNER, List.of(ChatMessageDeliveryStatus.SENT)))
                .thenReturn(List.of(pending));
        when(chatMessageRepository.findByRoom_IdAndDeletedFalse(eq(101L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending)));

        ChatMessagePageResponse response = chatService.listMessagesByOrderId(55L, 0, 30);

        assertEquals(1, response.getContent().size());
        assertEquals(501L, response.getContent().get(0).getMessageId());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void sendMessageShouldPersistAndMarkDeliveredWhenRecipientOnline() {
        ChatRoom room = room(101L, 55L, 11L, 77L);
        ChatParticipant customer = participant(room, ChatParticipantType.CUSTOMER, 11L);
        ChatParticipant partner = participant(room, ChatParticipantType.PARTNER, 77L);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(Optional.of(customer));
        when(chatParticipantRepository.findByRoom_IdAndDeletedFalse(101L)).thenReturn(List.of(customer, partner));
        when(simpUserRegistry.getUser("PARTNER:77")).thenReturn(mock(org.springframework.messaging.simp.user.SimpUser.class));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                ReflectionTestUtils.setField(message, "id", 501L);
                ReflectionTestUtils.setField(message, "createdAt", Instant.now());
                ReflectionTestUtils.setField(message, "updatedAt", Instant.now());
            }
            return message;
        });

        ChatSendMessageRequest request = new ChatSendMessageRequest();
        request.setRoomId(101L);
        request.setContent(" Hello partner ");
        request.setMessageType(ChatMessageType.TEXT);

        var response = chatService.sendMessage(request, new ChatActor(ChatParticipantType.CUSTOMER, 11L));

        assertEquals(501L, response.getMessageId());
        assertEquals(ChatMessageDeliveryStatus.DELIVERED, response.getDeliveryStatus());
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/chat/rooms/101"), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(eq("PARTNER:77"), eq("/queue/chat"), any());
    }

    @Test
    void sendMessageShouldRemainSentWhenRecipientOfflineAndTrimPreview() {
        ChatRoom room = room(102L, 56L, 11L, 77L);
        ChatParticipant customer = participant(room, ChatParticipantType.CUSTOMER, 11L);
        ChatParticipant partner = participant(room, ChatParticipantType.PARTNER, 77L);
        when(chatRoomRepository.findByIdAndDeletedFalse(102L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(102L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);
        when(chatParticipantRepository.findByRoom_IdAndDeletedFalse(102L)).thenReturn(List.of(customer, partner));
        when(simpUserRegistry.getUser("PARTNER:77")).thenReturn(null);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                ReflectionTestUtils.setField(message, "id", 601L);
                ReflectionTestUtils.setField(message, "createdAt", Instant.now());
                ReflectionTestUtils.setField(message, "updatedAt", Instant.now());
            }
            return message;
        });

        ChatSendMessageRequest request = new ChatSendMessageRequest();
        request.setRoomId(102L);
        request.setContent("x".repeat(700));
        request.setMessageType(ChatMessageType.TEXT);

        var response = chatService.sendMessage(request, new ChatActor(ChatParticipantType.CUSTOMER, 11L));

        assertEquals(601L, response.getMessageId());
        assertEquals(ChatMessageDeliveryStatus.SENT, response.getDeliveryStatus());
        assertEquals(500, room.getLastMessagePreview().length());
        verify(messagingTemplate, never()).convertAndSendToUser(eq("PARTNER:77"), eq("/queue/chat"), any());
    }

    @Test
    void sendMessageShouldRejectBlankAndOversizedContent() {
        ChatRoom room = room(101L, 55L, 11L, 77L);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);

        ChatSendMessageRequest blank = new ChatSendMessageRequest();
        blank.setRoomId(101L);
        blank.setContent("   ");
        blank.setMessageType(ChatMessageType.TEXT);

        BadRequestException blankEx = assertThrows(BadRequestException.class,
                () -> chatService.sendMessage(blank, new ChatActor(ChatParticipantType.CUSTOMER, 11L)));
        assertEquals("Message content is required", blankEx.getMessage());

        ChatSendMessageRequest oversized = new ChatSendMessageRequest();
        oversized.setRoomId(101L);
        oversized.setContent("x".repeat(2001));
        oversized.setMessageType(ChatMessageType.TEXT);

        BadRequestException oversizedEx = assertThrows(BadRequestException.class,
                () -> chatService.sendMessage(oversized, new ChatActor(ChatParticipantType.CUSTOMER, 11L)));
        assertEquals("Message content exceeds 2000 characters", oversizedEx.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void markMessageReadShouldResetUnreadCountAndPublishReadEvent() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("PARTNER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(77L));
        ChatRoom room = room(101L, 55L, 11L, 77L);
        ChatMessage message = message(room, 501L, ChatParticipantType.CUSTOMER, 11L, ChatMessageDeliveryStatus.DELIVERED);
        ChatParticipant partner = participant(room, ChatParticipantType.PARTNER, 77L);
        partner.setUnreadCount(3L);
        when(chatMessageRepository.findByIdAndDeletedFalse(501L)).thenReturn(Optional.of(message));
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(true);
        when(chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.PARTNER, 77L))
                .thenReturn(Optional.of(partner));
        when(chatMessageRepository.findByRoom_IdAndSenderTypeNotAndDeletedFalseAndIdLessThanEqualAndDeliveryStatusIn(
                eq(101L), eq(ChatParticipantType.PARTNER), eq(501L), any()))
                .thenReturn(List.of(message));

        ChatReadReceiptResponse response = chatService.markMessageRead(501L);

        assertEquals(0L, partner.getUnreadCount());
        assertEquals(501L, response.getMessageId());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/rooms/101"), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(eq("PARTNER:77"), eq("/queue/chat"), any());
    }

    @Test
    void listMessagesShouldRejectUnauthorizedRoomAccess() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        ChatRoom room = room(101L, 55L, 11L, 77L);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> chatService.listMessages(101L, 0, 20));
        verify(chatMessageRepository, never()).findByRoom_IdAndDeletedFalse(eq(101L), any(Pageable.class));
    }

    @Test
    void sendMessageShouldRejectInvalidParticipantLayout() {
        ChatRoom room = room(103L, 57L, 11L, 77L);
        ChatParticipant customer = participant(room, ChatParticipantType.CUSTOMER, 11L);
        when(chatRoomRepository.findByIdAndDeletedFalse(103L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(103L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);
        when(chatParticipantRepository.findByRoom_IdAndDeletedFalse(103L)).thenReturn(List.of(customer));

        ChatSendMessageRequest request = new ChatSendMessageRequest();
        request.setRoomId(103L);
        request.setContent("hello");
        request.setMessageType(ChatMessageType.TEXT);

        assertThrows(IllegalStateException.class,
                () -> chatService.sendMessage(request, new ChatActor(ChatParticipantType.CUSTOMER, 11L)));
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void publishTypingShouldWriteRedisKeyAndBroadcast() {
        ChatRoom room = room(101L, 55L, 11L, 77L);
        ChatParticipant customer = participant(room, ChatParticipantType.CUSTOMER, 11L);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);

        chatService.publishTyping(101L, new ChatActor(ChatParticipantType.CUSTOMER, 11L), ChatEventType.TYPING_START);

        verify(valueOperations).set(eq("chat:typing:101:CUSTOMER:11"), eq("1"), any());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/rooms/101"), any(Object.class));
    }

    @Test
    void publishTypingShouldDeleteRedisKeyOnStopAndRejectUnsupportedEvent() {
        ChatRoom room = room(101L, 55L, 11L, 77L);
        when(chatRoomRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(room));
        when(chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(101L, ChatParticipantType.CUSTOMER, 11L))
                .thenReturn(true);

        chatService.publishTyping(101L, new ChatActor(ChatParticipantType.CUSTOMER, 11L), ChatEventType.TYPING_STOP);

        verify(redisTemplate).delete("chat:typing:101:CUSTOMER:11");
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/rooms/101"), any(Object.class));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> chatService.publishTyping(101L, new ChatActor(ChatParticipantType.CUSTOMER, 11L), ChatEventType.CHAT_MESSAGE));
        assertEquals("Unsupported typing event", ex.getMessage());
    }

    private CreateChatRoomRequest request(Long orderId) {
        CreateChatRoomRequest request = new CreateChatRoomRequest();
        request.setOrderId(orderId);
        return request;
    }

    private ChatRoom room(Long id, Long orderId, Long customerId, Long partnerId) {
        ChatRoom room = new ChatRoom();
        ReflectionTestUtils.setField(room, "id", id);
        room.setOrderId(orderId);
        room.setCustomerId(customerId);
        room.setPartnerId(partnerId);
        ReflectionTestUtils.setField(room, "createdAt", Instant.now());
        ReflectionTestUtils.setField(room, "updatedAt", Instant.now());
        return room;
    }

    private ChatParticipant participant(ChatRoom room, ChatParticipantType type, Long id) {
        ChatParticipant participant = new ChatParticipant();
        participant.setRoom(room);
        participant.setParticipantType(type);
        participant.setParticipantId(id);
        ReflectionTestUtils.setField(participant, "createdAt", Instant.now());
        ReflectionTestUtils.setField(participant, "updatedAt", Instant.now());
        return participant;
    }

    private ChatMessage message(ChatRoom room,
                                Long id,
                                ChatParticipantType senderType,
                                Long senderId,
                                ChatMessageDeliveryStatus status) {
        ChatMessage message = new ChatMessage();
        ReflectionTestUtils.setField(message, "id", id);
        message.setRoom(room);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setMessageType(ChatMessageType.TEXT);
        message.setContent("hello");
        message.setDeliveryStatus(status);
        ReflectionTestUtils.setField(message, "createdAt", Instant.now());
        ReflectionTestUtils.setField(message, "updatedAt", Instant.now());
        return message;
    }
}
