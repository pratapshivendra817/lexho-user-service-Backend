package com.lexho.user.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.lexho.user.entity.CallSession;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.repository.CallSessionRepository;
import com.lexho.user.service.CallRealtimePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
@ConditionalOnProperty(name = "socket.io.enabled", havingValue = "true", matchIfMissing = true)
public class SocketCallRealtimePublisher implements CallRealtimePublisher {

    private static final EnumSet<CallStatus> ACTIVE_STATUSES = EnumSet.of(
            CallStatus.INITIATED,
            CallStatus.RINGING,
            CallStatus.CONNECTED
    );

    private final SocketSessionRegistry sessionRegistry;
    private final CallSessionRepository callSessionRepository;
    private final String stunUrl;
    private final String turnUrl;
    private final String turnUsername;
    private final String turnCredential;

    public SocketCallRealtimePublisher(SocketSessionRegistry sessionRegistry,
                                       CallSessionRepository callSessionRepository,
                                       @Value("${calling.ice.stun-url:stun:stun.l.google.com:19302}") String stunUrl,
                                       @Value("${calling.ice.turn-url:}") String turnUrl,
                                       @Value("${calling.ice.turn-username:}") String turnUsername,
                                       @Value("${calling.ice.turn-credential:}") String turnCredential) {
        this.sessionRegistry = sessionRegistry;
        this.callSessionRepository = callSessionRepository;
        this.stunUrl = stunUrl;
        this.turnUrl = turnUrl;
        this.turnUsername = turnUsername;
        this.turnCredential = turnCredential;
    }

    @Override
    public void publishIncoming(CallSession callSession) {
        emitToUser(callSession.getReceiverId(), "call:incoming", sessionPayload(callSession, null));
    }

    @Override
    public void publishRinging(CallSession callSession) {
        emitToUser(callSession.getCallerId(), "call:ringing", sessionPayload(callSession, null));
    }

    @Override
    public void publishAccepted(CallSession callSession) {
        emitToParticipants(callSession, "call:accept");
    }

    @Override
    public void publishRejected(CallSession callSession) {
        emitToParticipants(callSession, "call:reject");
    }

    @Override
    public void publishBusy(CallSession callSession) {
        emitToUser(callSession.getCallerId(), "call:busy", sessionPayload(callSession, null));
    }

    @Override
    public void publishEnded(CallSession callSession) {
        emitToParticipants(callSession, "call:end");
    }

    @Override
    public void publishTimeout(CallSession callSession) {
        emitToParticipants(callSession, "call:timeout");
    }

    @Override
    public void relayOffer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        emitToUser(toUserId, "call:offer", new SignalEventPayload(sessionId, fromUserId, toUserId, payload));
    }

    @Override
    public void relayAnswer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        emitToUser(toUserId, "call:answer", new SignalEventPayload(sessionId, fromUserId, toUserId, payload));
    }

    @Override
    public void relayIceCandidate(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        emitToUser(toUserId, "call:ice-candidate", new SignalEventPayload(sessionId, fromUserId, toUserId, payload));
    }

    @Override
    public void syncActiveState(Long userId) {
        java.util.List<CallSession> activeSessions =
                callSessionRepository.findByParticipantAndStatuses(userId, ACTIVE_STATUSES);
        for (CallSession session : activeSessions) {
            if (session.getStatus() == CallStatus.CONNECTED) {
                emitToUser(userId, "call:accept", sessionPayload(session, null));
            } else if (session.getReceiverId().equals(userId)) {
                emitToUser(userId, "call:incoming", sessionPayload(session, null));
            } else {
                emitToUser(userId, "call:ringing", sessionPayload(session, null));
            }
        }
    }

    private SocketEventPayload sessionPayload(CallSession session, Object data) {
        return new SocketEventPayload(
                session.getId(),
                session.getCallerId(),
                session.getReceiverId(),
                session.getStatus(),
                session.getDuration(),
                stunUrl,
                turnUrl,
                turnUsername,
                turnCredential,
                data
        );
    }

    private void emitToParticipants(CallSession session, String event) {
        SocketEventPayload payload = sessionPayload(session, null);
        emitToUser(session.getCallerId(), event, payload);
        emitToUser(session.getReceiverId(), event, payload);
    }

    private void emitToUser(Long userId, String event, Object payload) {
        for (SocketIOClient client : sessionRegistry.getUserSockets(userId)) {
            client.sendEvent(event, payload);
        }
    }
}
