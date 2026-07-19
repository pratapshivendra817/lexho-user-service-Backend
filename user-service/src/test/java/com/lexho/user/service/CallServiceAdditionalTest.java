package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.dto.CallHistoryResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallServiceAdditionalTest {

    private static final EnumSet<CallStatus> ACTIVE_STATUSES = EnumSet.of(
            CallStatus.INITIATED,
            CallStatus.RINGING,
            CallStatus.CONNECTED
    );

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

    private CallService service;
    private AuthenticatedUser customer;

    @BeforeEach
    void setUp() {
        customer = new AuthenticatedUser(10L, "9999999999", Role.CUSTOMER);
        service = new CallService(
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
    void initiateCallRejectsSelfCall() {
        assertThatThrownBy(() -> service.initiateCall(customer, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Caller and receiver must be different users");
    }

    @Test
    void initiateCallRejectsUnsupportedCallerRole() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", Role.ADMIN);

        assertThatThrownBy(() -> service.initiateCall(admin, 20L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only customers and partners can place calls");
    }

    @Test
    void initiateCallRejectsMissingReceiver() {
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.initiateCall(customer, 20L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Receiver does not exist");
    }

    @Test
    void initiateCallRejectsInactiveReceiver() {
        User inactiveReceiver = user(20L, Role.PARTNER, UserStatus.BLOCKED, false);
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(inactiveReceiver));

        assertThatThrownBy(() -> service.initiateCall(customer, 20L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Receiver is inactive");
    }

    @Test
    void initiateCallRejectsBusyCaller() {
        when(userRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(user(20L, Role.PARTNER, UserStatus.ACTIVE, true)));
        when(callSessionRepository.findByParticipantAndStatuses(10L, ACTIVE_STATUSES))
                .thenReturn(List.of(session(1L, 10L, 20L, CallStatus.CONNECTED)));

        assertThatThrownBy(() -> service.initiateCall(customer, 20L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Caller is already in an active call");
    }

    @Test
    void acceptCallRejectsNonReceiver() {
        CallSession session = session(11L, 10L, 20L, CallStatus.RINGING);
        when(callSessionRepository.findByIdAndParticipant(11L, 10L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.acceptCall(customer, 11L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the receiver can accept this call");
    }

    @ParameterizedTest
    @EnumSource(value = CallStatus.class, names = {"CONNECTED", "REJECTED", "BUSY", "MISSED", "ENDED"})
    void acceptCallRejectsUnavailableStatuses(CallStatus status) {
        AuthenticatedUser receiver = new AuthenticatedUser(20L, "8888888888", Role.PARTNER);
        when(callSessionRepository.findByIdAndParticipant(11L, 20L))
                .thenReturn(Optional.of(session(11L, 10L, 20L, status)));

        assertThatThrownBy(() -> service.acceptCall(receiver, 11L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Call is not available to accept");
    }

    @Test
    void rejectCallRejectsAlreadyClosedSession() {
        AuthenticatedUser receiver = new AuthenticatedUser(20L, "8888888888", Role.PARTNER);
        when(callSessionRepository.findByIdAndParticipant(12L, 20L))
                .thenReturn(Optional.of(session(12L, 10L, 20L, CallStatus.ENDED)));

        assertThatThrownBy(() -> service.rejectCall(receiver, 12L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Call is already closed");
    }

    @Test
    void markBusyRejectsNonReceiver() {
        CallSession callSession = session(13L, 10L, 20L, CallStatus.RINGING);
        when(callSessionRepository.findByIdAndParticipant(13L, 10L)).thenReturn(Optional.of(callSession));

        assertThatThrownBy(() -> service.markBusy(customer, 13L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the receiver can mark the call busy");
    }

    @Test
    void endCallReturnsCurrentStateWhenAlreadyClosed() {
        CallSession callSession = session(14L, 10L, 20L, CallStatus.ENDED);
        when(callSessionRepository.findByIdAndParticipant(14L, 10L)).thenReturn(Optional.of(callSession));

        var response = service.endCall(customer, 14L);

        assertThat(response.status()).isEqualTo(CallStatus.ENDED);
        verify(callSessionRepository, never()).save(any(CallSession.class));
        verify(realtimePublisher, never()).publishEnded(any(CallSession.class));
    }

    @Test
    void markMissedCallReturnsWhenAlreadyConnected() {
        when(callSessionRepository.findById(15L))
                .thenReturn(Optional.of(session(15L, 10L, 20L, CallStatus.CONNECTED)));

        service.markMissedCall(15L);

        verify(callSessionRepository, never()).save(any(CallSession.class));
        verify(realtimePublisher, never()).publishTimeout(any(CallSession.class));
    }

    @Test
    void markMissedCallThrowsWhenSessionMissing() {
        when(callSessionRepository.findById(16L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markMissedCall(16L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Call session not found");
    }

    @Test
    void relayOfferRejectsInactiveCall() {
        when(callSessionRepository.findByIdAndParticipant(17L, 10L))
                .thenReturn(Optional.of(session(17L, 10L, 20L, CallStatus.ENDED)));

        assertThatThrownBy(() -> service.relayOffer(customer, 17L, "payload"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Call is not active");
    }

    @Test
    void relaySignalsDispatchToCorrectPublisherMethods() {
        CallSession activeSession = session(18L, 10L, 20L, CallStatus.CONNECTED);
        when(callSessionRepository.findByIdAndParticipant(18L, 10L)).thenReturn(Optional.of(activeSession));

        service.relayOffer(customer, 18L, "offer");
        service.relayAnswer(customer, 18L, "answer");
        service.relayIceCandidate(customer, 18L, "ice");

        verify(realtimePublisher).relayOffer(18L, 10L, 20L, "offer");
        verify(realtimePublisher).relayAnswer(18L, 10L, 20L, "answer");
        verify(realtimePublisher).relayIceCandidate(18L, 10L, 20L, "ice");
    }

    @Test
    void getHistoryMapsPagedResponse() {
        PageRequest pageRequest = PageRequest.of(0, 5);
        when(callSessionRepository.findHistoryByUserId(10L, pageRequest))
                .thenReturn(new PageImpl<>(List.of(session(19L, 10L, 20L, CallStatus.MISSED)), pageRequest, 1));

        var response = service.getHistory(customer, 0, 5);

        assertThat(response.content()).hasSize(1);
        CallHistoryResponse item = response.content().get(0);
        assertThat(item.id()).isEqualTo(19L);
        assertThat(item.status()).isEqualTo(CallStatus.MISSED);
    }

    @Test
    void getCallFindsOwnedSession() {
        when(callSessionRepository.findByIdAndParticipant(20L, 10L))
                .thenReturn(Optional.of(session(20L, 10L, 20L, CallStatus.CONNECTED)));

        var response = service.getCall(customer, 20L);

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.turnUrl()).isEqualTo("turn:test");
    }

    @Test
    void getMissedCallsMapsSessions() {
        when(callSessionRepository.findMissedCalls(10L))
                .thenReturn(List.of(session(21L, 10L, 20L, CallStatus.MISSED)));

        assertThat(service.getMissedCalls(customer))
                .extracting(CallHistoryResponse::id)
                .containsExactly(21L);
    }

    @Test
    void syncAndFindActiveCallsDelegateToCollaborators() {
        List<CallSession> activeCalls = List.of(session(22L, 10L, 20L, CallStatus.RINGING));
        when(callSessionRepository.findByParticipantAndStatuses(10L, ACTIVE_STATUSES)).thenReturn(activeCalls);

        service.syncActiveCalls(10L);
        List<CallSession> result = service.findActiveCallsForUser(10L);

        verify(realtimePublisher).syncActiveState(10L);
        assertThat(result).isSameAs(activeCalls);
    }

    @Test
    void acceptCallUpdatesExistingLog() {
        AuthenticatedUser receiver = new AuthenticatedUser(20L, "8888888888", Role.PARTNER);
        CallSession activeSession = session(23L, 10L, 20L, CallStatus.RINGING);
        CallLog existingLog = new CallLog();
        existingLog.setSessionId(23L);

        when(callSessionRepository.findByIdAndParticipant(23L, 20L)).thenReturn(Optional.of(activeSession));
        when(callSessionRepository.findByParticipantAndStatuses(20L, ACTIVE_STATUSES)).thenReturn(List.of(activeSession));
        when(callSessionRepository.save(any(CallSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(callLogRepository.findBySessionId(23L)).thenReturn(Optional.of(existingLog));
        when(callLogRepository.save(any(CallLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.acceptCall(receiver, 23L);

        assertThat(existingLog.getCallType()).isEqualTo(CallType.AUDIO);
        assertThat(existingLog.getStatus()).isEqualTo(CallStatus.CONNECTED);
        verify(callLogRepository).save(existingLog);
    }

    @ParameterizedTest
    @ValueSource(strings = {"offer", "answer", "ice"})
    void getOwnedSessionThrowsWhenSessionMissing(String ignored) {
        when(callSessionRepository.findByIdAndParticipant(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCall(customer, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Call session not found");
    }

    private static User user(Long id, Role role, UserStatus status, boolean active) {
        User user = new User();
        user.setFullName("Receiver");
        user.setPhoneNumber("9999999999");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(status);
        if (user.isActive() != active) {
            user.setActive(active);
        }
        user.setTotalBookings(0L);
        user.setDeleted(false);
        setId(user, id);
        return user;
    }

    private static CallSession session(Long id, Long callerId, Long receiverId, CallStatus status) {
        CallSession session = new CallSession();
        session.setCallerId(callerId);
        session.setReceiverId(receiverId);
        session.setStatus(status);
        session.setStartTime(LocalDateTime.now().minusSeconds(30));
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
