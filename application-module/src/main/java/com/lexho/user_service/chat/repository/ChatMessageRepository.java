package com.lexho.user_service.chat.repository;

import com.lexho.user_service.chat.entity.ChatMessage;
import com.lexho.user_service.chat.enums.ChatMessageDeliveryStatus;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByRoom_IdAndDeletedFalse(Long roomId, Pageable pageable);

    Optional<ChatMessage> findByIdAndDeletedFalse(Long id);

    List<ChatMessage> findByRoom_IdAndSenderTypeNotAndDeletedFalseAndDeliveryStatusIn(Long roomId,
                                                                                       ChatParticipantType senderType,
                                                                                       Collection<ChatMessageDeliveryStatus> deliveryStatuses);

    List<ChatMessage> findByRoom_IdAndSenderTypeNotAndDeletedFalseAndIdLessThanEqualAndDeliveryStatusIn(Long roomId,
                                                                                                         ChatParticipantType senderType,
                                                                                                         Long messageId,
                                                                                                         Collection<ChatMessageDeliveryStatus> deliveryStatuses);
}
