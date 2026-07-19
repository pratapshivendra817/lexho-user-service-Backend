package com.lexho.user_service.chat.repository;

import com.lexho.user_service.chat.entity.ChatRoom;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByOrderIdAndDeletedFalse(Long orderId);

    Optional<ChatRoom> findByIdAndDeletedFalse(Long id);

    @Query("""
            select r from ChatRoom r
            where r.deleted = false
              and (
                (:participantType = com.lexho.user_service.chat.enums.ChatParticipantType.CUSTOMER and r.customerId = :participantId)
                or
                (:participantType = com.lexho.user_service.chat.enums.ChatParticipantType.PARTNER and r.partnerId = :participantId)
              )
            """)
    Page<ChatRoom> findVisibleRooms(@Param("participantType") ChatParticipantType participantType,
                                    @Param("participantId") Long participantId,
                                    Pageable pageable);
}
