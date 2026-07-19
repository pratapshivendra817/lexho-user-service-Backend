package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.dto.CallHistoryResponse;
import com.lexho.user.dto.CallSessionResponse;
import com.lexho.user.dto.PagedResponse;
import com.lexho.user.entity.CallLog;
import com.lexho.user.entity.CallSession;
import com.lexho.user.entity.User;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.enums.CallType;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.BadRequestException;
import com.lexho.user.exception.ConflictException;
import com.lexho.user.exception.ForbiddenException;
import com.lexho.user.exception.ResourceNotFoundException;
import com.lexho.user.repository.CallLogRepository;
import com.lexho.user.repository.CallSessionRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

@Service
public class CallService {

    private static final EnumSet<CallStatus> ACTIVE_STATUSES = EnumSet.of(
            CallStatus.INITIATED,
            CallStatus.RINGING,
            CallStatus.CONNECTED
    );

    private final CallSessionRepository callSessionRepository;
    private final CallLogRepository callLogRepository;
    private final UserRepository userRepository;
    private final CallRealtimePublisher realtimePublisher;
    private final CallTimeoutScheduler timeoutScheduler;
    private final RateLimiterService rateLimiterService;
    private final Duration ringingTimeout;
    private final String stunUrl;
    private final String turnUrl;
    private final String turnUsername;
    private final String turnCredential;

    public CallService(CallSessionRepository callSessionRepository,
                       CallLogRepository callLogRepository,
                       UserRepository userRepository,
                       CallRealtimePublisher realtimePublisher,
                       CallTimeoutScheduler timeoutScheduler,
                       RateLimiterService rateLimiterService,
                       @Value("${calling.timeout-seconds:30}") long timeoutSeconds,
                       @Value("${calling.ice.stun-url:stun:stun.l.google.com:19302}") String stunUrl,
                       @Value("${calling.ice.turn-url:}") String turnUrl,
                       @Value("${calling.ice.turn-username:}") String turnUsername,
                       @Value("${calling.ice.turn-credential:}") String turnCredential) {
        this.callSessionRepository = callSessionRepository;
        this.callLogRepository = callLogRepository;
        this.userRepository = userRepository;
        this.realtimePublisher = realtimePublisher;
        this.timeoutScheduler = timeoutScheduler;
        this.rateLimiterService = rateLimiterService;
        this.ringingTimeout = Duration.ofSeconds(timeoutSeconds);
        this.stunUrl = stunUrl;
        this.turnUrl = turnUrl;
        this.turnUsername = turnUsername;
        this.turnCredential = turnCredential;
    }

    @Transactional
    public CallSessionResponse initiateCall(AuthenticatedUser caller, Long receiverId) {
        if (caller.userId().equals(receiverId)) {
            throw new BadRequestException("Caller and receiver must be different users");
        }
        ensureCallRole(caller.role());
        User receiver = findActiveUser(receiverId);
        ensureCallRole(receiver.getRole());
        rateLimiterService.check("call-initiate", caller.userId().toString(), 10, Duration.ofMinutes(1),
                "Call initiation rate limit exceeded");

        ensureUserNotBusy(caller.userId(), "Caller is already in an active call");
        if (hasActiveCall(receiverId)) {
            CallSession busySession = new CallSession();
            busySession.setCallerId(caller.userId());
            busySession.setReceiverId(receiverId);
            busySession.setStatus(CallStatus.BUSY);
            busySession.setEndTime(LocalDateTime.now());
            busySession = callSessionRepository.save(busySession);
            upsertLog(busySession);
            realtimePublisher.publishBusy(busySession);
            return toResponse(busySession);
        }

        CallSession session = new CallSession();
        session.setCallerId(caller.userId());
        session.setReceiverId(receiverId);
        session.setStatus(CallStatus.INITIATED);
        session = callSessionRepository.save(session);

        session.setStatus(CallStatus.RINGING);
        session = callSessionRepository.save(session);
        upsertLog(session);
        scheduleTimeout(session.getId());
        realtimePublisher.publishIncoming(session);
        realtimePublisher.publishRinging(session);
        return toResponse(session);
    }

    @Transactional
    public CallSessionResponse acceptCall(AuthenticatedUser user, Long sessionId) {
        rateLimiterService.check("call-accept", user.userId().toString(), 20, Duration.ofMinutes(1),
                "Call acceptance rate limit exceeded");
        CallSession session = requireReceiverSession(sessionId, user.userId(), "Only the receiver can accept this call");
        if (!isPendingAcceptance(session)) {
            throw new ConflictException("Call is not available to accept");
        }
        ensureUserNotBusyForConnect(user.userId(), sessionId);
        session.setStatus(CallStatus.CONNECTED);
        session.setAnswerTime(LocalDateTime.now());
        session = saveSession(session);
        timeoutScheduler.cancel(sessionId);
        realtimePublisher.publishAccepted(session);
        return toResponse(session);
    }

    @Transactional
    public CallSessionResponse rejectCall(AuthenticatedUser user, Long sessionId) {
        return closeReceiverCall(
                user,
                sessionId,
                CallStatus.REJECTED,
                "Only the receiver can reject this call",
                realtimePublisher::publishRejected
        );
    }

    @Transactional
    public CallSessionResponse markBusy(AuthenticatedUser user, Long sessionId) {
        return closeReceiverCall(
                user,
                sessionId,
                CallStatus.BUSY,
                "Only the receiver can mark the call busy",
                realtimePublisher::publishBusy
        );
    }

    @Transactional
    public CallSessionResponse endCall(AuthenticatedUser user, Long sessionId) {
        CallSession session = getOwnedSession(sessionId, user.userId());
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            return toResponse(session);
        }
        session = closeSession(session, CallStatus.ENDED);
        timeoutScheduler.cancel(sessionId);
        realtimePublisher.publishEnded(session);
        return toResponse(session);
    }

    @Transactional
    public void markMissedCall(Long sessionId) {
        CallSession session = getSessionById(sessionId);
        if (!isPendingAcceptance(session)) {
            return;
        }
        session = closeSession(session, CallStatus.MISSED);
        realtimePublisher.publishTimeout(session);
    }

    @Transactional
    public void relayOffer(AuthenticatedUser user, Long sessionId, Object payload) {
        relaySignal(sessionId, user.userId(), payload, SignalType.OFFER);
    }

    @Transactional
    public void relayAnswer(AuthenticatedUser user, Long sessionId, Object payload) {
        relaySignal(sessionId, user.userId(), payload, SignalType.ANSWER);
    }

    @Transactional
    public void relayIceCandidate(AuthenticatedUser user, Long sessionId, Object payload) {
        relaySignal(sessionId, user.userId(), payload, SignalType.ICE);
    }

    public PagedResponse<CallHistoryResponse> getHistory(AuthenticatedUser user, int page, int size) {
        Page<CallSession> history = callSessionRepository.findHistoryByUserId(user.userId(), PageRequest.of(page, size));
        return new PagedResponse<>(
                history.getContent().stream().map(this::toHistoryResponse).toList(),
                history.getTotalElements(),
                history.getTotalPages(),
                history.getNumber(),
                history.getSize()
        );
    }

    public CallSessionResponse getCall(AuthenticatedUser user, Long sessionId) {
        return toResponse(getOwnedSession(sessionId, user.userId()));
    }

    public List<CallHistoryResponse> getMissedCalls(AuthenticatedUser user) {
        return mapHistory(callSessionRepository.findMissedCalls(user.userId()));
    }

    public void syncActiveCalls(Long userId) {
        realtimePublisher.syncActiveState(userId);
    }

    public List<CallSession> findActiveCallsForUser(Long userId) {
        return callSessionRepository.findByParticipantAndStatuses(userId, ACTIVE_STATUSES);
    }

    private void relaySignal(Long sessionId, Long fromUserId, Object payload, SignalType signalType) {
        CallSession session = getOwnedSession(sessionId, fromUserId);
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            throw new ConflictException("Call is not active");
        }
        Long toUserId = session.otherParticipant(fromUserId);
        switch (signalType) {
            case OFFER -> realtimePublisher.relayOffer(sessionId, fromUserId, toUserId, payload);
            case ANSWER -> realtimePublisher.relayAnswer(sessionId, fromUserId, toUserId, payload);
            case ICE -> realtimePublisher.relayIceCandidate(sessionId, fromUserId, toUserId, payload);
        }
    }

    private void scheduleTimeout(Long sessionId) {
        timeoutScheduler.schedule(sessionId, ringingTimeout, () -> markMissedCall(sessionId));
    }

    private void ensureUserNotBusy(Long userId, String message) {
        if (hasActiveCall(userId)) {
            throw new ConflictException(message);
        }
    }

    private void ensureUserNotBusyForConnect(Long userId, Long allowedSessionId) {
        boolean busyElsewhere = callSessionRepository.findByParticipantAndStatuses(userId, ACTIVE_STATUSES).stream()
                .anyMatch(session -> !session.getId().equals(allowedSessionId) && session.getStatus() == CallStatus.CONNECTED);
        if (busyElsewhere) {
            throw new ConflictException("User is already connected on another call");
        }
    }

    private boolean hasActiveCall(Long userId) {
        return !callSessionRepository.findByParticipantAndStatuses(userId, ACTIVE_STATUSES).isEmpty();
    }

    private CallSessionResponse closeReceiverCall(AuthenticatedUser user,
                                                  Long sessionId,
                                                  CallStatus nextStatus,
                                                  String forbiddenMessage,
                                                  Consumer<CallSession> publisher) {
        CallSession session = requireReceiverSession(sessionId, user.userId(), forbiddenMessage);
        if (!ACTIVE_STATUSES.contains(session.getStatus())) {
            throw new ConflictException("Call is already closed");
        }
        session = closeSession(session, nextStatus);
        timeoutScheduler.cancel(sessionId);
        publisher.accept(session);
        return toResponse(session);
    }

    private CallSession requireReceiverSession(Long sessionId, Long userId, String forbiddenMessage) {
        CallSession session = getOwnedSession(sessionId, userId);
        if (!session.getReceiverId().equals(userId)) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return session;
    }

    private boolean isPendingAcceptance(CallSession session) {
        return ACTIVE_STATUSES.contains(session.getStatus()) && session.getStatus() != CallStatus.CONNECTED;
    }

    private CallSession closeSession(CallSession session, CallStatus nextStatus) {
        session.complete(nextStatus, LocalDateTime.now());
        return saveSession(session);
    }

    private CallSession saveSession(CallSession session) {
        CallSession savedSession = callSessionRepository.save(session);
        upsertLog(savedSession);
        return savedSession;
    }

    private CallSession getSessionById(Long sessionId) {
        return callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));
    }

    private CallSession getOwnedSession(Long sessionId, Long userId) {
        return callSessionRepository.findByIdAndParticipant(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Call session not found"));
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver does not exist"));
        if (user.getStatus() != UserStatus.ACTIVE || !user.isActive()) {
            throw new ConflictException("Receiver is inactive");
        }
        return user;
    }

    private void ensureCallRole(Role role) {
        if (role != Role.CUSTOMER && role != Role.PARTNER) {
            throw new ForbiddenException("Only customers and partners can place calls");
        }
    }

    private void upsertLog(CallSession session) {
        CallLog log = callLogRepository.findBySessionId(session.getId()).orElseGet(CallLog::new);
        log.setSessionId(session.getId());
        log.setCallerId(session.getCallerId());
        log.setReceiverId(session.getReceiverId());
        log.setCallType(CallType.AUDIO);
        log.setStatus(session.getStatus());
        log.setDuration(session.getDuration());
        callLogRepository.save(log);
    }

    private CallSessionResponse toResponse(CallSession session) {
        return new CallSessionResponse(
                session.getId(),
                session.getCallerId(),
                session.getReceiverId(),
                session.getStatus(),
                session.getStartTime(),
                session.getAnswerTime(),
                session.getEndTime(),
                session.getDuration(),
                stunUrl,
                turnUrl,
                turnUsername,
                turnCredential
        );
    }

    private CallHistoryResponse toHistoryResponse(CallSession session) {
        return new CallHistoryResponse(
                session.getId(),
                session.getCallerId(),
                session.getReceiverId(),
                session.getStatus(),
                session.getStartTime(),
                session.getAnswerTime(),
                session.getEndTime(),
                session.getDuration()
        );
    }

    private List<CallHistoryResponse> mapHistory(List<CallSession> sessions) {
        return sessions.stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private enum SignalType {
        OFFER,
        ANSWER,
        ICE
    }
}
