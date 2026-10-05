package com.lexho.user_service.chat.repository;

import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatMessageType;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = ChatRepositoryTest.JpaConfig.class)
class ChatRepositoryTest {

    @Autowired
    private ChatRoomRepository roomRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    void roomParticipantAndMessageQueriesFilterDeletedAndVisibilityCorrectly() {
        ChatRoom customerRoom = roomRepository.save(room(1001L, 501L, 601L, false, Instant.now().minusSeconds(60)));
        ChatRoom deletedRoom = roomRepository.save(room(1002L, 501L, 602L, true, Instant.now().minusSeconds(10)));

        ChatParticipant customer = participantRepository.save(participant(customerRoom, ChatParticipantType.CUSTOMER, 501L, false, 1));
        participantRepository.save(participant(customerRoom, ChatParticipantType.PARTNER, 601L, false, 2));
        participantRepository.save(participant(customerRoom, ChatParticipantType.CUSTOMER, 999L, true, 0));
        participantRepository.save(participant(deletedRoom, ChatParticipantType.CUSTOMER, 501L, false, 3));

        ChatMessage sent = messageRepository.save(message(customerRoom, ChatParticipantType.CUSTOMER, 501L, ChatMessageDeliveryStatus.SENT, false, "Need help"));
        ChatMessage delivered = messageRepository.save(message(customerRoom, ChatParticipantType.PARTNER, 601L, ChatMessageDeliveryStatus.DELIVERED, false, "On the way"));
        ChatMessage read = messageRepository.save(message(customerRoom, ChatParticipantType.PARTNER, 601L, ChatMessageDeliveryStatus.READ, false, "Reached"));
        messageRepository.save(message(customerRoom, ChatParticipantType.PARTNER, 601L, ChatMessageDeliveryStatus.SENT, true, "Deleted copy"));

        assertThat(roomRepository.findByOrderIdAndDeletedFalse(1001L)).contains(customerRoom);
        assertThat(roomRepository.findByOrderIdAndDeletedFalse(1002L)).isEmpty();
        assertThat(roomRepository.findByIdAndDeletedFalse(deletedRoom.getId())).isEmpty();

        assertThat(roomRepository.findVisibleRooms(ChatParticipantType.CUSTOMER, 501L, PageRequest.of(0, 10)).getContent())
                .extracting(ChatRoom::getId)
                .containsExactly(customerRoom.getId());
        assertThat(roomRepository.findVisibleRooms(ChatParticipantType.PARTNER, 601L, PageRequest.of(0, 10)).getContent())
                .extracting(ChatRoom::getId)
                .containsExactly(customerRoom.getId());

        assertThat(participantRepository.findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(
                customerRoom.getId(), ChatParticipantType.CUSTOMER, 501L
        )).contains(customer);
        assertThat(participantRepository.existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(
                customerRoom.getId(), ChatParticipantType.CUSTOMER, 999L
        )).isFalse();
        assertThat(participantRepository.findByRoom_IdAndDeletedFalse(customerRoom.getId()))
                .extracting(ChatParticipant::getParticipantId)
                .containsExactlyInAnyOrder(501L, 601L);
        assertThat(participantRepository.findByRoom_IdInAndParticipantTypeAndParticipantIdAndDeletedFalse(
                List.of(customerRoom.getId(), deletedRoom.getId()), ChatParticipantType.CUSTOMER, 501L
        )).extracting(ChatParticipant::getParticipantId).containsExactly(501L, 501L);

        assertThat(messageRepository.findByRoom_IdAndDeletedFalse(customerRoom.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(ChatMessage::getId)
                .containsExactly(sent.getId(), delivered.getId(), read.getId());
        assertThat(messageRepository.findByIdAndDeletedFalse(sent.getId())).contains(sent);
        assertThat(messageRepository.findByRoom_IdAndSenderTypeNotAndDeletedFalseAndDeliveryStatusIn(
                customerRoom.getId(),
                ChatParticipantType.CUSTOMER,
                List.of(ChatMessageDeliveryStatus.SENT, ChatMessageDeliveryStatus.DELIVERED)
        )).extracting(ChatMessage::getId).containsExactly(delivered.getId());
        assertThat(messageRepository.findByRoom_IdAndSenderTypeNotAndDeletedFalseAndIdLessThanEqualAndDeliveryStatusIn(
                customerRoom.getId(),
                ChatParticipantType.CUSTOMER,
                delivered.getId(),
                List.of(ChatMessageDeliveryStatus.SENT, ChatMessageDeliveryStatus.DELIVERED, ChatMessageDeliveryStatus.READ)
        )).extracting(ChatMessage::getId).containsExactly(delivered.getId());
    }

    private ChatRoom room(Long orderId, Long customerId, Long partnerId, boolean deleted, Instant lastMessageAt) {
        ChatRoom room = new ChatRoom();
        room.setOrderId(orderId);
        room.setCustomerId(customerId);
        room.setPartnerId(partnerId);
        room.setLastMessagePreview("hello");
        room.setLastMessageAt(lastMessageAt);
        room.setDeleted(deleted);
        room.setDeletedAt(deleted ? Instant.now() : null);
        return room;
    }

    private ChatParticipant participant(ChatRoom room, ChatParticipantType type, Long participantId, boolean deleted, long unreadCount) {
        ChatParticipant participant = new ChatParticipant();
        participant.setRoom(room);
        participant.setParticipantType(type);
        participant.setParticipantId(participantId);
        participant.setUnreadCount(unreadCount);
        participant.setDeleted(deleted);
        participant.setDeletedAt(deleted ? Instant.now() : null);
        return participant;
    }

    private ChatMessage message(ChatRoom room,
                                ChatParticipantType senderType,
                                Long senderId,
                                ChatMessageDeliveryStatus status,
                                boolean deleted,
                                String content) {
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setMessageType(ChatMessageType.TEXT);
        message.setContent(content);
        message.setDeliveryStatus(status);
        message.setDeleted(deleted);
        message.setDeletedAt(deleted ? Instant.now() : null);
        return message;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ChatRoom.class, ChatParticipant.class, ChatMessage.class})
    @EnableJpaRepositories(
            basePackageClasses = {ChatRoomRepository.class, ChatParticipantRepository.class, ChatMessageRepository.class},
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ChatRoomRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ChatParticipantRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ChatMessageRepository.class)
            }
    )
    static class JpaConfig {
    }
}

