package com.lexho.user.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.lexho.user.security.AuthenticatedUser;
import com.lexho.user.service.CallService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "socket.io.enabled", havingValue = "true", matchIfMissing = true)
public class CallSocketHandler {

    private final SocketAuthService socketAuthService;
    private final SocketSessionRegistry sessionRegistry;
    private final CallService callService;

    public CallSocketHandler(SocketIOServer socketIOServer,
                             SocketAuthService socketAuthService,
                             SocketSessionRegistry sessionRegistry,
                             CallService callService) {
        this.socketAuthService = socketAuthService;
        this.sessionRegistry = sessionRegistry;
        this.callService = callService;
        registerListeners(socketIOServer);
    }

    private void registerListeners(SocketIOServer socketIOServer) {
        socketIOServer.addConnectListener(client -> {
            SocketPrincipal principal = socketAuthService.authenticate(client.getHandshakeData());
            sessionRegistry.register(client, principal);
            callService.syncActiveCalls(principal.userId());
        });

        socketIOServer.addDisconnectListener(sessionRegistry::unregister);

        registerActionListener(socketIOServer, "call:initiate", CallInitiateSocketRequest.class,
                (user, request) -> callService.initiateCall(user, request.receiverId()));
        registerActionListener(socketIOServer, "call:accept", CallActionSocketRequest.class,
                (user, request) -> callService.acceptCall(user, request.sessionId()));
        registerActionListener(socketIOServer, "call:reject", CallActionSocketRequest.class,
                (user, request) -> callService.rejectCall(user, request.sessionId()));
        registerActionListener(socketIOServer, "call:busy", CallActionSocketRequest.class,
                (user, request) -> callService.markBusy(user, request.sessionId()));
        registerActionListener(socketIOServer, "call:end", CallActionSocketRequest.class,
                (user, request) -> callService.endCall(user, request.sessionId()));
        registerActionListener(socketIOServer, "call:offer", SignalSocketRequest.class,
                (user, request) -> callService.relayOffer(user, request.sessionId(), request.data()));
        registerActionListener(socketIOServer, "call:answer", SignalSocketRequest.class,
                (user, request) -> callService.relayAnswer(user, request.sessionId(), request.data()));
        registerActionListener(socketIOServer, "call:ice-candidate", SignalSocketRequest.class,
                (user, request) -> callService.relayIceCandidate(user, request.sessionId(), request.data()));
    }

    private AuthenticatedUser toAuthenticatedUser(SocketPrincipal principal) {
        return new AuthenticatedUser(principal.userId(), principal.subject(), principal.role());
    }

    private <T> void registerActionListener(SocketIOServer socketIOServer,
                                            String event,
                                            Class<T> payloadType,
                                            SocketRequestHandler<T> handler) {
        socketIOServer.addEventListener(event, payloadType, toDataListener(handler));
    }

    private <T> DataListener<T> toDataListener(SocketRequestHandler<T> handler) {
        return (client, request, ackSender) -> {
            SocketPrincipal principal = sessionRegistry.requirePrincipal(client);
            handler.handle(toAuthenticatedUser(principal), request);
        };
    }

    @FunctionalInterface
    private interface SocketRequestHandler<T> {
        void handle(AuthenticatedUser user, T request);
    }
}
