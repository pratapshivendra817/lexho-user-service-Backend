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
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishRinging(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishAccepted(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishRejected(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishBusy(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishEnded(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void publishTimeout(CallSession callSession) {
        // Socket.IO is disabled for this profile, so no realtime event is emitted.
    }

    @Override
    public void relayOffer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        // Socket.IO is disabled for this profile, so signaling payloads are intentionally not relayed.
    }

    @Override
    public void relayAnswer(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        // Socket.IO is disabled for this profile, so signaling payloads are intentionally not relayed.
    }

    @Override
    public void relayIceCandidate(Long sessionId, Long fromUserId, Long toUserId, Object payload) {
        // Socket.IO is disabled for this profile, so signaling payloads are intentionally not relayed.
    }

    @Override
    public void syncActiveState(Long userId) {
        // Socket.IO is disabled for this profile, so active-call state is not pushed over sockets.
    }
}
