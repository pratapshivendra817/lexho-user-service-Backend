package com.lexho.user.socket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.lexho.common.enums.Role;
import com.lexho.user.entity.CallSession;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.repository.CallSessionRepository;
import com.lexho.user.security.AuthenticatedUser;
import com.lexho.user.security.JwtService;
import com.lexho.user.service.CallService;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocketComponentsTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CallSessionRepository callSessionRepository;

    @Mock
    private SocketIOServer socketIOServer;

    @Mock
    private CallService callService;

    @Test
    void socketAuthServiceAuthenticatesHeaderAndQueryToken() {
        SocketAuthService service = new SocketAuthService(jwtService);
        HandshakeData handshakeData = mock(HandshakeData.class);
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.set("Authorization", "Bearer signed-token");

        when(handshakeData.getHttpHeaders()).thenReturn(headers);
        when(jwtService.parse("signed-token")).thenReturn(new AuthenticatedUser(5L, "9999999999", Role.CUSTOMER));

        SocketPrincipal principal = service.authenticate(handshakeData);

        assertThat(principal.userId()).isEqualTo(5L);
        assertThat(principal.role()).isEqualTo(Role.CUSTOMER);

        HandshakeData queryHandshake = mock(HandshakeData.class);
        when(queryHandshake.getHttpHeaders()).thenReturn(new DefaultHttpHeaders());
        when(queryHandshake.getSingleUrlParam("token")).thenReturn(" query-token ");
        when(jwtService.parse("query-token")).thenReturn(new AuthenticatedUser(6L, "8888888888", Role.PARTNER));

        SocketPrincipal queryPrincipal = service.authenticate(queryHandshake);
        assertThat(queryPrincipal.userId()).isEqualTo(6L);
        assertThat(queryPrincipal.role()).isEqualTo(Role.PARTNER);
    }

    @Test
    void socketAuthServiceRejectsMissingTokenAndUnsupportedRole() {
        SocketAuthService service = new SocketAuthService(jwtService);
        HandshakeData handshakeData = mock(HandshakeData.class);
        when(handshakeData.getHttpHeaders()).thenReturn(new DefaultHttpHeaders());
        when(handshakeData.getSingleUrlParam("token")).thenReturn(" ");

        assertThatThrownBy(() -> service.authenticate(handshakeData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Socket token is required");

        HandshakeData adminHandshake = mock(HandshakeData.class);
        HttpHeaders adminHeaders = new DefaultHttpHeaders();
        adminHeaders.set("Authorization", "Bearer admin-token");
        when(adminHandshake.getHttpHeaders()).thenReturn(adminHeaders);
        when(jwtService.parse("admin-token")).thenReturn(new AuthenticatedUser(7L, "admin", Role.ADMIN));

        assertThatThrownBy(() -> service.authenticate(adminHandshake))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported role");
    }

    @Test
    void socketSessionRegistryRegistersRequiresAndUnregistersPrincipals() {
        SocketSessionRegistry registry = new SocketSessionRegistry();
        SocketIOClient client = mock(SocketIOClient.class);
        UUID sessionId = UUID.randomUUID();
        when(client.getSessionId()).thenReturn(sessionId);

        SocketPrincipal principal = new SocketPrincipal(11L, Role.CUSTOMER, "9999999999");
        registry.register(client, principal);

        assertThat(registry.requirePrincipal(client)).isEqualTo(principal);
        assertThat(registry.getUserSockets(11L)).containsExactly(client);

        registry.unregister(client);

        assertThat(registry.getUserSockets(11L)).isEmpty();
        assertThatThrownBy(() -> registry.requirePrincipal(client))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Socket principal not found");
    }

    @Test
    void socketCallRealtimePublisherEmitsExpectedEventsAndActiveState() {
        SocketSessionRegistry registry = new SocketSessionRegistry();
        SocketIOClient callerClient = socketClient(UUID.randomUUID());
        SocketIOClient receiverClient = socketClient(UUID.randomUUID());
        registry.register(callerClient, new SocketPrincipal(101L, Role.CUSTOMER, "caller"));
        registry.register(receiverClient, new SocketPrincipal(202L, Role.PARTNER, "receiver"));

        SocketCallRealtimePublisher publisher = new SocketCallRealtimePublisher(
                registry, callSessionRepository, "stun:test", "turn:test", "user", "secret"
        );
        CallSession ringing = session(1L, 101L, 202L, CallStatus.RINGING);
        CallSession connected = session(2L, 101L, 202L, CallStatus.CONNECTED);

        publisher.publishIncoming(ringing);
        publisher.publishRinging(ringing);
        publisher.publishAccepted(connected);
        publisher.publishRejected(ringing);
        publisher.publishBusy(ringing);
        publisher.publishEnded(connected);
        publisher.publishTimeout(ringing);
        publisher.relayOffer(9L, 101L, 202L, Map.of("sdp", "offer"));
        publisher.relayAnswer(9L, 202L, 101L, Map.of("sdp", "answer"));
        publisher.relayIceCandidate(9L, 202L, 101L, Map.of("candidate", "ice"));

        when(callSessionRepository.findByParticipantAndStatuses(202L,
                EnumSet.of(CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED)))
                .thenReturn(List.of(connected, ringing));

        publisher.syncActiveState(202L);

        verify(receiverClient, org.mockito.Mockito.atLeastOnce()).sendEvent(eq("call:incoming"), any(SocketEventPayload.class));
        verify(callerClient, org.mockito.Mockito.atLeastOnce()).sendEvent(eq("call:ringing"), any(SocketEventPayload.class));
        verify(callerClient, org.mockito.Mockito.atLeastOnce()).sendEvent(eq("call:accept"), any(SocketEventPayload.class));
        verify(receiverClient, org.mockito.Mockito.atLeastOnce()).sendEvent(eq("call:accept"), any(SocketEventPayload.class));
        verify(callerClient).sendEvent(eq("call:reject"), any(SocketEventPayload.class));
        verify(receiverClient).sendEvent(eq("call:reject"), any(SocketEventPayload.class));
        verify(callerClient).sendEvent(eq("call:busy"), any(SocketEventPayload.class));
        verify(callerClient).sendEvent(eq("call:end"), any(SocketEventPayload.class));
        verify(receiverClient).sendEvent(eq("call:end"), any(SocketEventPayload.class));
        verify(callerClient).sendEvent(eq("call:timeout"), any(SocketEventPayload.class));
        verify(receiverClient).sendEvent(eq("call:timeout"), any(SocketEventPayload.class));
        verify(receiverClient).sendEvent(eq("call:offer"), any(SignalEventPayload.class));
        verify(callerClient).sendEvent(eq("call:answer"), any(SignalEventPayload.class));
        verify(callerClient).sendEvent(eq("call:ice-candidate"), any(SignalEventPayload.class));
    }

    @Test
    void callSocketHandlerRegistersListenersAndDispatchesEventsToService() throws Exception {
        SocketAuthService socketAuthService = mock(SocketAuthService.class);
        SocketSessionRegistry sessionRegistry = mock(SocketSessionRegistry.class);
        SocketIOClient client = mock(SocketIOClient.class);
        AckRequest ackRequest = mock(AckRequest.class);
        HandshakeData handshakeData = mock(HandshakeData.class);
        SocketPrincipal principal = new SocketPrincipal(77L, Role.PARTNER, "8888888888");

        when(client.getHandshakeData()).thenReturn(handshakeData);
        when(socketAuthService.authenticate(handshakeData)).thenReturn(principal);
        when(sessionRegistry.requirePrincipal(client)).thenReturn(principal);

        ArgumentCaptor<ConnectListener> connectCaptor = ArgumentCaptor.forClass(ConnectListener.class);
        ArgumentCaptor<DisconnectListener> disconnectCaptor = ArgumentCaptor.forClass(DisconnectListener.class);
        Map<String, DataListener<?>> listeners = new java.util.HashMap<>();

        doAnswer(invocation -> {
            listeners.put(invocation.getArgument(0), invocation.getArgument(2));
            return null;
        }).when(socketIOServer).addEventListener(any(String.class), any(), any());

        new CallSocketHandler(socketIOServer, socketAuthService, sessionRegistry, callService);

        verify(socketIOServer).addConnectListener(connectCaptor.capture());
        verify(socketIOServer).addDisconnectListener(disconnectCaptor.capture());

        connectCaptor.getValue().onConnect(client);
        disconnectCaptor.getValue().onDisconnect(client);

        verify(sessionRegistry).register(client, principal);
        verify(callService).syncActiveCalls(77L);
        verify(sessionRegistry).unregister(client);

        @SuppressWarnings("unchecked")
        DataListener<CallInitiateSocketRequest> initiateListener =
                (DataListener<CallInitiateSocketRequest>) listeners.get("call:initiate");
        initiateListener.onData(client, new CallInitiateSocketRequest(99L), ackRequest);
        verify(callService).initiateCall(new AuthenticatedUser(77L, "8888888888", Role.PARTNER), 99L);

        @SuppressWarnings("unchecked")
        DataListener<CallActionSocketRequest> acceptListener =
                (DataListener<CallActionSocketRequest>) listeners.get("call:accept");
        acceptListener.onData(client, new CallActionSocketRequest(1001L), ackRequest);
        verify(callService).acceptCall(new AuthenticatedUser(77L, "8888888888", Role.PARTNER), 1001L);

        @SuppressWarnings("unchecked")
        DataListener<SignalSocketRequest> offerListener =
                (DataListener<SignalSocketRequest>) listeners.get("call:offer");
        offerListener.onData(client, new SignalSocketRequest(1002L, Map.of("sdp", "offer")), ackRequest);
        verify(callService).relayOffer(new AuthenticatedUser(77L, "8888888888", Role.PARTNER), 1002L, Map.of("sdp", "offer"));
    }

    @Test
    void noOpPublisherAndSocketIoPropertiesAreSafeDefaults() {
        NoOpCallRealtimePublisher publisher = new NoOpCallRealtimePublisher();
        CallSession session = session(3L, 1L, 2L, CallStatus.CONNECTED);

        assertThatCode(() -> {
            publisher.publishIncoming(session);
            publisher.publishRinging(session);
            publisher.publishAccepted(session);
            publisher.publishRejected(session);
            publisher.publishBusy(session);
            publisher.publishEnded(session);
            publisher.publishTimeout(session);
            publisher.relayOffer(1L, 1L, 2L, "offer");
            publisher.relayAnswer(1L, 2L, 1L, "answer");
            publisher.relayIceCandidate(1L, 2L, 1L, "ice");
            publisher.syncActiveState(1L);
        }).doesNotThrowAnyException();

        SocketIoProperties properties = new SocketIoProperties();
        assertThat(properties.getHost()).isEqualTo("0.0.0.0");
        assertThat(properties.getPort()).isEqualTo(9092);
        properties.setHost("127.0.0.1");
        properties.setPort(9093);
        assertThat(properties.getHost()).isEqualTo("127.0.0.1");
        assertThat(properties.getPort()).isEqualTo(9093);
        assertThat(new SocketPrincipal(1L, Role.CUSTOMER, "subject").subject()).isEqualTo("subject");
        assertThat(new CallInitiateSocketRequest(8L).receiverId()).isEqualTo(8L);
        assertThat(new CallActionSocketRequest(9L).sessionId()).isEqualTo(9L);
        assertThat(new SignalSocketRequest(10L, "payload").data()).isEqualTo("payload");
        assertThat(new SignalEventPayload(11L, 1L, 2L, "signal").data()).isEqualTo("signal");
        assertThat(new SocketEventPayload(12L, 1L, 2L, CallStatus.CONNECTED, 30L, "stun", "turn", "u", "c", "data").status())
                .isEqualTo(CallStatus.CONNECTED);
    }

    @Test
    void socketIoConfigAuthorizationListenerDelegatesToAuthService() {
        SocketIoProperties properties = new SocketIoProperties();
        properties.setHost("localhost");
        properties.setPort(9099);
        SocketAuthService socketAuthService = mock(SocketAuthService.class);
        HandshakeData handshakeData = mock(HandshakeData.class);

        SocketIoConfig config = new SocketIoConfig();
        AuthorizationResult result = invokeAuthorizationListener(config, socketAuthService, handshakeData);

        assertThat(result.isAuthorized()).isTrue();
        verify(socketAuthService).authenticate(handshakeData);
    }

    private static AuthorizationResult invokeAuthorizationListener(SocketIoConfig config,
                                                                   SocketAuthService socketAuthService,
                                                                   HandshakeData handshakeData) {
        try {
            java.lang.reflect.Method method = SocketIoConfig.class
                    .getDeclaredMethod("authorizationListener", SocketAuthService.class);
            method.setAccessible(true);
            com.corundumstudio.socketio.AuthorizationListener listener =
                    (com.corundumstudio.socketio.AuthorizationListener) method.invoke(config, socketAuthService);
            return listener.getAuthorizationResult(handshakeData);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static SocketIOClient socketClient(UUID sessionId) {
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(sessionId);
        return client;
    }

    private static CallSession session(Long id, Long callerId, Long receiverId, CallStatus status) {
        CallSession session = new CallSession();
        session.setCallerId(callerId);
        session.setReceiverId(receiverId);
        session.setStatus(status);
        session.setDuration(10L);
        session.setStartTime(LocalDateTime.now().minusSeconds(20));
        setId(session, id);
        return session;
    }

    private static void setId(CallSession session, Long id) {
        try {
            java.lang.reflect.Field idField = CallSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
