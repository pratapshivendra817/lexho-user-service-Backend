package com.lexho.user.repository;

import com.lexho.user.entity.CallSession;
import com.lexho.user.enums.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

    @Query("""
            select cs from CallSession cs
            where (cs.callerId = :userId or cs.receiverId = :userId)
            order by cs.startTime desc
            """)
    Page<CallSession> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select cs from CallSession cs
            where cs.receiverId = :userId and cs.status = 'MISSED'
            order by cs.startTime desc
            """)
    List<CallSession> findMissedCalls(@Param("userId") Long userId);

    @Query("""
            select cs from CallSession cs
            where (cs.callerId = :userId or cs.receiverId = :userId)
              and cs.status in :statuses
            order by cs.startTime desc
            """)
    List<CallSession> findByParticipantAndStatuses(@Param("userId") Long userId,
                                                   @Param("statuses") Collection<CallStatus> statuses);

    @Query("""
            select cs from CallSession cs
            where cs.id = :id and (cs.callerId = :userId or cs.receiverId = :userId)
            """)
    Optional<CallSession> findByIdAndParticipant(@Param("id") Long id, @Param("userId") Long userId);
}
