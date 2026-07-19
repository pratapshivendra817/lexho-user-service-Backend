package com.lexho.user_service.chat.repository;

import com.lexho.user_service.chat.entity.ChatParticipant;
import com.lexho.user_service.chat.enums.ChatParticipantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    Optional<ChatParticipant> findByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(Long roomId,
                                                                                              ChatParticipantType participantType,
                                                                                              Long participantId);

    boolean existsByRoom_IdAndParticipantTypeAndParticipantIdAndDeletedFalse(Long roomId,
                                                                             ChatParticipantType participantType,
                                                                             Long participantId);

    List<ChatParticipant> findByRoom_IdAndDeletedFalse(Long roomId);

    List<ChatParticipant> findByRoom_IdInAndParticipantTypeAndParticipantIdAndDeletedFalse(Collection<Long> roomIds,
                                                                                            ChatParticipantType participantType,
                                                                                            Long participantId);
}
