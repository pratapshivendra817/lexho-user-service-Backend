package com.lexho.user.entity;

import com.lexho.user.enums.CallStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_session",
        indexes = {
                @Index(name = "idx_call_session_caller_status", columnList = "caller_id,status"),
                @Index(name = "idx_call_session_receiver_status", columnList = "receiver_id,status"),
                @Index(name = "idx_call_session_start_time", columnList = "start_time")
        })
public class CallSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caller_id", nullable = false)
    private Long callerId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "answer_time")
    private LocalDateTime answerTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Long duration;

    @PrePersist
    public void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (status == null) {
            status = CallStatus.INITIATED;
        }
        if (duration == null) {
            duration = 0L;
        }
    }

    public void complete(CallStatus nextStatus, LocalDateTime now) {
        this.status = nextStatus;
        this.endTime = now;
        LocalDateTime durationStart = answerTime != null ? answerTime : startTime;
        this.duration = durationStart == null ? 0L : Math.max(0L, Duration.between(durationStart, now).getSeconds());
    }

    public boolean isParticipant(Long userId) {
        return callerId.equals(userId) || receiverId.equals(userId);
    }

    public Long otherParticipant(Long userId) {
        return callerId.equals(userId) ? receiverId : callerId;
    }

    public Long getId() {
        return id;
    }

    public Long getCallerId() {
        return callerId;
    }

    public void setCallerId(Long callerId) {
        this.callerId = callerId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getAnswerTime() {
        return answerTime;
    }

    public void setAnswerTime(LocalDateTime answerTime) {
        this.answerTime = answerTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
