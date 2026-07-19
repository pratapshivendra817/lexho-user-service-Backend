package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.entity.CallLog;
import com.lexho.user.entity.CallSession;
import com.lexho.user.entity.User;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.ConflictException;
import com.lexho.user.repository.CallLogRepository;
import com.lexho.user.repository.CallSessionRepository;
import com.lexho.user.repository.UserRepository;
import com.lexho.user.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallServiceTest {

    @Mock
    private CallSessionRepository callSessionRepository;

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CallRealtimePublisher realtimePublisher;

    @Mock
    private CallTimeoutScheduler timeoutScheduler;

    @Mock
    private RateLimiterService rateLimiterService;

    private CallService callService;

    private AuthenticatedUser caller;
    private User receiver;

    @BeforeEach
    void setUp() {
        caller = new AuthenticatedUser(10L, "9999999999", Role.CUSTOMER);
        receiver = activeUser(20L, Role.PARTNER);
        callService = new CallService(
                callSessionRepository,
                callLogRepository,
                userRepository,
                realtimePublisher,
                timeoutScheduler,
                rateLimiterService,
                30L,
                "stun:test",
                "turn:test",
                "turn-user",
                "turn-secret"
        );
    }

    @Test
    void initiateCallCreatesRingingSessionAndSchedulesTimeout() {
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(receiver));
        when(callSessionRepository.findByParticipantAndStatuses(10L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of());
        when(callSessionRepository.findByParticipantAndStatuses(20L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of());
        when(callSessionRepository.save(any(CallSession.class)))
                .thenAnswer(invocation -> {
                    CallSession session = invocation.getArgument(0);
                    if (session.getId() == null) {
                        setId(session, 101L);
                    }
                    if (session.getStartTime() == null) {
                        session.setStartTime(LocalDateTime.now());
                    }
                    if (session.getDuration() == null) {
                        session.setDuration(0L);
                    }
                    return session;
                });
        when(callLogRepository.findBySessionId(101L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = callService.initiateCall(caller, 20L);

        assertThat(response.status()).isEqualTo(CallStatus.RINGING);
        assertThat(response.stunUrl()).isEqualTo("stun:test");
        verify(timeoutScheduler).schedule(any(Long.class), any(), any());
        verify(realtimePublisher).publishIncoming(any(CallSession.class));
        verify(realtimePublisher).publishRinging(any(CallSession.class));
    }

    @Test
    void initiateCallReturnsBusyWhenReceiverAlreadyActive() {
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(receiver));
        when(callSessionRepository.findByParticipantAndStatuses(10L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of());
        when(callSessionRepository.findByParticipantAndStatuses(20L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of(new CallSession()));
        when(callSessionRepository.save(any(CallSession.class)))
                .thenAnswer(invocation -> {
                    CallSession session = invocation.getArgument(0);
                    setId(session, 201L);
                    if (session.getStartTime() == null) {
                        session.setStartTime(LocalDateTime.now());
                    }
                    if (session.getDuration() == null) {
                        session.setDuration(0L);
                    }
                    return session;
                });
        when(callLogRepository.findBySessionId(201L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = callService.initiateCall(caller, 20L);

        assertThat(response.status()).isEqualTo(CallStatus.BUSY);
        verify(realtimePublisher).publishBusy(any(CallSession.class));
        verify(timeoutScheduler, never()).schedule(any(Long.class), any(), any());
    }

    @Test
    void acceptCallTransitionsToConnected() {
        CallSession session = activeSession(301L, 10L, 20L, CallStatus.RINGING);
        when(callSessionRepository.findByIdAndParticipant(301L, 20L)).thenReturn(Optional.of(session));
        when(callSessionRepository.findByParticipantAndStatuses(20L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of(session));
        when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callLogRepository.findBySessionId(301L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = callService.acceptCall(new AuthenticatedUser(20L, "8888888888", Role.PARTNER), 301L);

        assertThat(response.status()).isEqualTo(CallStatus.CONNECTED);
        verify(timeoutScheduler).cancel(301L);
        verify(realtimePublisher).publishAccepted(session);
    }

    @Test
    void rejectCallTransitionsToRejected() {
        CallSession session = activeSession(302L, 10L, 20L, CallStatus.RINGING);
        when(callSessionRepository.findByIdAndParticipant(302L, 20L)).thenReturn(Optional.of(session));
        when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callLogRepository.findBySessionId(302L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = callService.rejectCall(new AuthenticatedUser(20L, "8888888888", Role.PARTNER), 302L);

        assertThat(response.status()).isEqualTo(CallStatus.REJECTED);
        verify(realtimePublisher).publishRejected(session);
    }

    @Test
    void endCallTransitionsToEndedAndPersistsDuration() {
        CallSession session = activeSession(303L, 10L, 20L, CallStatus.CONNECTED);
        session.setAnswerTime(LocalDateTime.now().minusSeconds(15));
        when(callSessionRepository.findByIdAndParticipant(303L, 10L)).thenReturn(Optional.of(session));
        when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callLogRepository.findBySessionId(303L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = callService.endCall(caller, 303L);

        assertThat(response.status()).isEqualTo(CallStatus.ENDED);
        assertThat(response.duration()).isGreaterThanOrEqualTo(15L);
        verify(realtimePublisher).publishEnded(session);
    }

    @Test
    void acceptCallFailsWhenUserAlreadyConnectedElsewhere() {
        CallSession target = activeSession(304L, 10L, 20L, CallStatus.RINGING);
        CallSession other = activeSession(305L, 30L, 20L, CallStatus.CONNECTED);
        when(callSessionRepository.findByIdAndParticipant(304L, 20L)).thenReturn(Optional.of(target));
        when(callSessionRepository.findByParticipantAndStatuses(20L, java.util.EnumSet.of(
                CallStatus.INITIATED, CallStatus.RINGING, CallStatus.CONNECTED))).thenReturn(List.of(target, other));

        assertThatThrownBy(() -> callService.acceptCall(new AuthenticatedUser(20L, "8888888888", Role.PARTNER), 304L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already connected");
    }

    @Test
    void markMissedCallUpdatesStatus() {
        CallSession session = activeSession(306L, 10L, 20L, CallStatus.RINGING);
        when(callSessionRepository.findById(306L)).thenReturn(Optional.of(session));
        when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callLogRepository.findBySessionId(306L)).thenReturn(Optional.empty());
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        callService.markMissedCall(306L);

        assertThat(session.getStatus()).isEqualTo(CallStatus.MISSED);
        verify(realtimePublisher).publishTimeout(session);
    }

    private static User activeUser(Long id, Role role) {
        User user = new User();
        user.setFullName("Receiver");
        user.setPhoneNumber("9999999999");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setTotalBookings(0L);
        user.setDeleted(false);
        setId(user, id);
        return user;
    }

    private static CallSession activeSession(Long id, Long callerId, Long receiverId, CallStatus status) {
        CallSession session = new CallSession();
        session.setCallerId(callerId);
        session.setReceiverId(receiverId);
        session.setStatus(status);
        session.setStartTime(LocalDateTime.now().minusSeconds(20));
        session.setDuration(0L);
        setId(session, id);
        return session;
    }

    private static void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
