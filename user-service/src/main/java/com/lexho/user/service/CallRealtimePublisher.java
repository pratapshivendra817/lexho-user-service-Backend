package com.lexho.user.service;

import com.lexho.user.entity.CallSession;

public interface CallRealtimePublisher {

    void publishIncoming(CallSession callSession);

    void publishRinging(CallSession callSession);

    void publishAccepted(CallSession callSession);

    void publishRejected(CallSession callSession);

    void publishBusy(CallSession callSession);

    void publishEnded(CallSession callSession);

    void publishTimeout(CallSession callSession);

    void relayOffer(Long sessionId, Long fromUserId, Long toUserId, Object payload);

    void relayAnswer(Long sessionId, Long fromUserId, Long toUserId, Object payload);

    void relayIceCandidate(Long sessionId, Long fromUserId, Long toUserId, Object payload);

    void syncActiveState(Long userId);
}
