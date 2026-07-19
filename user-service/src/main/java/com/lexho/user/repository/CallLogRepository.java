package com.lexho.user.repository;

import com.lexho.user.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    Optional<CallLog> findBySessionId(Long sessionId);
}
