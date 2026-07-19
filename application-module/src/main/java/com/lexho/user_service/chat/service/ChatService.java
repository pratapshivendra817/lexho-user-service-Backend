package com.lexho.user_service.chat.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.provider.repository.PartnerRepository;
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
import com.lexho.user_service.chat.dto.CreateChatRoomRequest;
import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatEventType;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import com.lexho.user_service.chat.model.ChatActor;
import com.lexho.user_service.chat.repository.ChatMessageRepository;
import com.lexho.user_service.chat.repository.ChatParticipantRepository;
import com.lexho.user_service.chat.repository.ChatRoomRepository;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.UnauthorizedException;
import com.lexho.user_service.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Duration TYPING_TTL = Duration.ofSeconds(10);
    private static final String ROOM_TOPIC_PREFIX = "/topic/chat/rooms/";
    private static final String USER_QUEUE_DESTINATION = "/queue/chat";
    private static final String TYPING_KEY_PREFIX = "chat:typing:";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PartnerRepository partnerRepository;
    private final SecurityUtils securityUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(ChatRoomRepository chatRoomRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       ChatMessageRepository chatMessageRepository,
                       OrderRepository orderRepository,
                       CustomerRepository customerRepository,
                       PartnerRepository partnerRepository,
                       SecurityUtils securityUtils,
                       RedisTemplate<String, String> redisTemplate,
                       SimpUserRegistry simpUserRegistry,
                       SimpMessagingTemplate messagingTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.partnerRepository = partnerRepository;
        this.securityUtils = securityUtils;
        this.redisTemplate = redisTemplate;
        this.simpUserRegistry = simpUserRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChatRoomResponse createRoom(CreateChatRoomRequest request) {
        ChatActor actor = currentActor();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateRoomCreationAccess(actor, order);

        ChatRoom existing = chatRoomRepository.findByOrderIdAndDeletedFalse(order.getId()).orElse(null);
        if (existing != null) {
            return toRoomResponse(existing, currentParticipant(existing.getId(), actor));
        }

        Long partnerId = order.getAssignedPartnerId();
        if (partnerId == null) {
            throw new BadRequestException("Partner is not assigned to this order");
        }
        if (!customerRepository.existsById(order.getUserId())) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (!partnerRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Partner not found");
        }

        ChatRoom room = new ChatRoom();
        room.setOrderId(order.getId());
        room.setCustomerId(order.getUserId());
        room.setPartnerId(partnerId);
        ChatRoom savedRoom = chatRoomRepository.save(room);

        chatParticipantRepository.save(createParticipant(savedRoom, ChatParticipantType.CUSTOMER, order.getUserId()));
        ChatParticipant actorParticipant = chatParticipantRepository.save(
                createParticipant(savedRoom, ChatParticipantType.PARTNER, partnerId));

        if (actor.participantType() == ChatParticipantType.CUSTOMER) {
            actorParticipant = chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(
                    savedRoom.getId(), actor.participantType(), actor.participantId()).orElseThrow();
        }

        return toRoomResponse(savedRoom, actorParticipant);
    }

    @Transactional(readOnly = true)
    public ChatRoomPageResponse listRooms(int page, int size) {
        ChatActor actor = currentActor();
        Page<ChatRoom> rooms = chatRoomRepository.findVisibleRooms(
                actor.participantType(),
                actor.participantId(),
                PageRequest.of(safePage(page), safeSize(size),
                        Sort.by(Sort.Order.desc("lastMessageAt"), Sort.Order.desc("createdAt"))));

        List<Long> roomIds = rooms.getContent().stream().map(ChatRoom::getId).toList();
        Map<Long, ChatParticipant> participantByRoomId = chatParticipantRepository
                .findByRoom_IdInAndParticipantTypeAndParticipantIdAndDeletedFalse(roomIds, actor.participantType(), actor.participantId())
                .stream()
                .collect(Collectors.toMap(participant -> participant.getRoom().getId(), Function.identity()));

        return ChatRoomPageResponse.builder()
                .content(rooms.getContent().stream()
                        .map(room -> toRoomResponse(room, participantByRoomId.get(room.getId())))
                        .toList())
                .page(rooms.getNumber())
                .size(rooms.getSize())
                .totalElements(rooms.getTotalElements())
                .totalPages(rooms.getTotalPages())
                .build();
    }

    @Transactional
    public ChatMessagePageResponse listMessages(Long roomId, int page, int size) {
        ChatActor actor = currentActor();
        ChatRoom room = requireRoomAccess(roomId, actor);
        ChatParticipant participant = currentParticipant(roomId, actor);
        markMessagesDelivered(room, actor);

        Page<ChatMessage> messages = chatMessageRepository.findByRoom_IdAndDeletedFalse(
                roomId,
                PageRequest.of(safePage(page), safeSize(size), Sort.by(Sort.Direction.DESC, "createdAt")));

        return ChatMessagePageResponse.builder()
                .content(messages.getContent().stream().map(this::toMessageResponse).toList())
                .page(messages.getNumber())
                .size(messages.getSize())
                .totalElements(messages.getTotalElements())
                .totalPages(messages.getTotalPages())
                .unreadCount(participant.getUnreadCount())
                .build();
    }

    @Transactional
    public ChatMessagePageResponse listMessagesByOrderId(Long orderId, int page, int size) {
        ChatRoomResponse room = createRoom(request(orderId));
        return listMessages(room.getRoomId(), page, size);
    }

    @Transactional
    public ChatReadReceiptResponse markMessageRead(Long messageId) {
        ChatActor actor = currentActor();
        ChatMessage message = chatMessageRepository.findByIdAndDeletedFalse(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat message not found"));
        ChatRoom room = requireRoomAccess(message.getRoom().getId(), actor);
        ChatParticipant participant = currentParticipant(room.getId(), actor);

        Instant now = Instant.now();
        List<ChatMessage> unreadMessages = chatMessageRepository
                .findByRoom_IdAndSenderTypeNotAndDeletedFalseAndIdLessThanEqualAndDeliveryStatusIn(
                        room.getId(),
                        actor.participantType(),
                        messageId,
                        List.of(ChatMessageDeliveryStatus.SENT, ChatMessageDeliveryStatus.DELIVERED))
                .stream()
                .peek(chatMessage -> {
                    chatMessage.setDeliveryStatus(ChatMessageDeliveryStatus.READ);
                    if (chatMessage.getDeliveredAt() == null) {
                        chatMessage.setDeliveredAt(now);
                    }
                    chatMessage.setReadAt(now);
                })
                .toList();
        chatMessageRepository.saveAll(unreadMessages);

        participant.setUnreadCount(0L);
        participant.setLastReadMessageId(messageId);
        participant.setLastReadAt(now);
        chatParticipantRepository.save(participant);

        publishMessageRead(room.getId(), messageId, actor, now);
        return ChatReadReceiptResponse.builder()
                .roomId(room.getId())
                .messageId(messageId)
                .readAt(now)
                .build();
    }

    @Transactional
    public ChatMessageResponse sendMessage(ChatSendMessageRequest request, ChatActor actor) {
        ChatRoom room = requireRoomAccess(request.getRoomId(), actor);
        String normalizedContent = normalizeMessageContent(request.getContent());

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSenderType(actor.participantType());
        message.setSenderId(actor.participantId());
        message.setMessageType(request.getMessageType());
        message.setContent(normalizedContent);
        message.setDeliveryStatus(ChatMessageDeliveryStatus.SENT);
        ChatMessage saved = chatMessageRepository.save(message);

        ChatParticipant recipient = otherParticipant(room, actor);
        recipient.setUnreadCount(recipient.getUnreadCount() + 1L);
        chatParticipantRepository.save(recipient);

        room.setLastMessagePreview(trimPreview(normalizedContent));
        room.setLastMessageAt(saved.getCreatedAt());
        chatRoomRepository.save(room);

        if (isOnline(new ChatActor(recipient.getParticipantType(), recipient.getParticipantId()))) {
            Instant deliveredAt = Instant.now();
            saved.setDeliveryStatus(ChatMessageDeliveryStatus.DELIVERED);
            saved.setDeliveredAt(deliveredAt);
            chatMessageRepository.save(saved);
            publishMessageDelivered(room.getId(), saved.getId(), recipient, deliveredAt);
        }

        ChatMessageResponse response = toMessageResponse(saved);
        publishChatMessage(room.getId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public boolean hasRoomAccess(Long roomId, ChatActor actor) {
        return chatParticipantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(
                roomId, actor.participantType(), actor.participantId());
    }

    public void publishTyping(Long roomId, ChatActor actor, ChatEventType eventType) {
        requireRoomAccess(roomId, actor);
        String key = typingKey(roomId, actor);
        if (eventType == ChatEventType.TYPING_START) {
            redisTemplate.opsForValue().set(key, "1", TYPING_TTL);
        } else if (eventType == ChatEventType.TYPING_STOP) {
            redisTemplate.delete(key);
        } else {
            throw new BadRequestException("Unsupported typing event");
        }

        ChatTypingEventResponse payload = ChatTypingEventResponse.builder()
                .roomId(roomId)
                .participantType(actor.participantType())
                .participantId(actor.participantId())
                .occurredAt(Instant.now())
                .build();
        messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, ChatSocketEvent.builder()
                .eventType(eventType)
                .roomId(roomId)
                .payload(payload)
                .occurredAt(payload.getOccurredAt())
                .build());
    }

    private ChatParticipant createParticipant(ChatRoom room, ChatParticipantType participantType, Long participantId) {
        ChatParticipant participant = new ChatParticipant();
        participant.setRoom(room);
        participant.setParticipantType(participantType);
        participant.setParticipantId(participantId);
        participant.setUnreadCount(0L);
        return participant;
    }

    private void validateRoomCreationAccess(ChatActor actor, Order order) {
        if (actor.participantType() == ChatParticipantType.CUSTOMER && !actor.participantId().equals(order.getUserId())) {
            throw new AccessDeniedException("Access denied to chat room");
        }
        if (actor.participantType() == ChatParticipantType.PARTNER) {
            if (order.getAssignedPartnerId() == null || !actor.participantId().equals(order.getAssignedPartnerId())) {
                throw new AccessDeniedException("Access denied to chat room");
            }
        }
    }

    private ChatRoom requireRoomAccess(Long roomId, ChatActor actor) {
        ChatRoom room = chatRoomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
        if (!hasRoomAccess(roomId, actor)) {
            throw new AccessDeniedException("Access denied to chat room");
        }
        return room;
    }

    private ChatParticipant currentParticipant(Long roomId, ChatActor actor) {
        return chatParticipantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(
                        roomId, actor.participantType(), actor.participantId())
                .orElseThrow(() -> new AccessDeniedException("Access denied to chat room"));
    }

    private ChatParticipant otherParticipant(ChatRoom room, ChatActor actor) {
        return chatParticipantRepository.findByRoom_IdAndDeletedFalse(room.getId()).stream()
                .filter(participant -> participant.getParticipantType() != actor.participantType()
                        || !participant.getParticipantId().equals(actor.participantId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Chat room participants are invalid"));
    }

    private void markMessagesDelivered(ChatRoom room, ChatActor actor) {
        ChatParticipant recipient = currentParticipant(room.getId(), actor);
        Instant now = Instant.now();
        List<ChatMessage> pendingMessages = chatMessageRepository
                .findByRoom_IdAndSenderTypeNotAndDeletedFalseAndDeliveryStatusIn(
                        room.getId(),
                        actor.participantType(),
                        List.of(ChatMessageDeliveryStatus.SENT))
                .stream()
                .peek(message -> {
                    message.setDeliveryStatus(ChatMessageDeliveryStatus.DELIVERED);
                    message.setDeliveredAt(now);
                })
                .toList();
        if (pendingMessages.isEmpty()) {
            return;
        }
        chatMessageRepository.saveAll(pendingMessages);
        for (ChatMessage pendingMessage : pendingMessages) {
            publishMessageDelivered(room.getId(), pendingMessage.getId(), recipient, now);
        }
    }

    private void publishChatMessage(Long roomId, ChatMessageResponse response) {
        Instant now = Instant.now();
        ChatSocketEvent<ChatMessageResponse> event = ChatSocketEvent.<ChatMessageResponse>builder()
                .eventType(ChatEventType.CHAT_MESSAGE)
                .roomId(roomId)
                .payload(response)
                .occurredAt(now)
                .build();
        messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, event);
    }

    private void publishMessageDelivered(Long roomId,
                                         Long messageId,
                                         ChatParticipant recipient,
                                         Instant deliveredAt) {
        ChatDeliveryReceiptResponse payload = ChatDeliveryReceiptResponse.builder()
                .roomId(roomId)
                .messageId(messageId)
                .deliveredAt(deliveredAt)
                .build();
        ChatSocketEvent<ChatDeliveryReceiptResponse> event = ChatSocketEvent.<ChatDeliveryReceiptResponse>builder()
                .eventType(ChatEventType.MESSAGE_DELIVERED)
                .roomId(roomId)
                .payload(payload)
                .occurredAt(deliveredAt)
                .build();
        messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, event);
        messagingTemplate.convertAndSendToUser(
                new ChatActor(recipient.getParticipantType(), recipient.getParticipantId()).principalName(),
                USER_QUEUE_DESTINATION,
                event);
    }

    private void publishMessageRead(Long roomId, Long messageId, ChatActor actor, Instant readAt) {
        ChatReadReceiptResponse payload = ChatReadReceiptResponse.builder()
                .roomId(roomId)
                .messageId(messageId)
                .readAt(readAt)
                .build();
        ChatSocketEvent<ChatReadReceiptResponse> event = ChatSocketEvent.<ChatReadReceiptResponse>builder()
                .eventType(ChatEventType.MESSAGE_READ)
                .roomId(roomId)
                .payload(payload)
                .occurredAt(readAt)
                .build();
        messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, event);
        messagingTemplate.convertAndSendToUser(actor.principalName(), USER_QUEUE_DESTINATION, event);
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, ChatParticipant selfParticipant) {
        long unreadCount = selfParticipant == null ? 0L : selfParticipant.getUnreadCount();
        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .orderId(room.getOrderId())
                .lastMessagePreview(room.getLastMessagePreview())
                .lastMessageAt(room.getLastMessageAt())
                .unreadCount(unreadCount)
                .participants(List.of(
                        new ChatParticipantSummary(ChatParticipantType.CUSTOMER, room.getCustomerId()),
                        new ChatParticipantSummary(ChatParticipantType.PARTNER, room.getPartnerId())
                ))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .deliveryStatus(message.getDeliveryStatus())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    private ChatActor currentActor() {
        String role = securityUtils.getCurrentRole()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
        Long userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Unauthorized"));
        return ChatActor.fromRole(role, userId);
    }

    private boolean isOnline(ChatActor actor) {
        return simpUserRegistry.getUser(actor.principalName()) != null;
    }

    private CreateChatRoomRequest request(Long orderId) {
        CreateChatRoomRequest request = new CreateChatRoomRequest();
        request.setOrderId(orderId);
        return request;
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeMessageContent(String content) {
        if (content == null) {
            throw new BadRequestException("Message content is required");
        }
        String normalized = content.trim();
        if (normalized.isEmpty()) {
            throw new BadRequestException("Message content is required");
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new BadRequestException("Message content exceeds 2000 characters");
        }
        return normalized;
    }

    private String trimPreview(String content) {
        return content.length() <= 500 ? content : content.substring(0, 500);
    }

    private String typingKey(Long roomId, ChatActor actor) {
        return TYPING_KEY_PREFIX + roomId + ":" + actor.principalName();
    }
}
