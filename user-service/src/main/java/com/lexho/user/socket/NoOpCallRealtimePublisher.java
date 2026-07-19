package com.lexho.user.socket;

import com.lexho.user.entity.CallSession;
import com.lexho.user.service.CallRealtimePublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "socket.io.enabled", havingValue = "false")
public class NoOpCallRealtimePublisher implements CallRealtimePublisher {

    @Override
    public void publishIncoming(CallSession callSession) {
    }

    @Override
    public void publishRinging(CallSession callSession) {
    }

    @Override
    public void publishAccepted(CallSession callSession) {
    }

    @Override
    public void publishRejected(CallSession callSession) {
    }

    @Override
    public void publishBusy(CallSession callSession) {
    }

    @Override
    public void publishEnded(CallSession callSession) {
    }

    @Override
    public void publishTimeout(CallSession callSession) {
    }

    @Override
    public void relayOffer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
    }

    @Override
    public void relayAnswer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
    }

    @Override
    public void relayIceCandidate(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
    }

    @Override
    public void syncActiveState(Long userId) {
    }
}
