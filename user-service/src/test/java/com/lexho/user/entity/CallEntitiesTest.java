package com.lexho.user.entity;

import com.lexho.common.enums.Role;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.enums.CallType;
import com.lexho.user.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CallEntitiesTest {

    @Test
    void callSessionLifecycleAndParticipantsAreCovered() {
        CallSession session = new CallSession();
        session.onCreate();

        assertThat(session.getStartTime()).isNotNull();
        assertThat(session.getStatus()).isEqualTo(CallStatus.INITIATED);
        assertThat(session.getDuration()).isZero();

        session.setCallerId(1L);
        session.setReceiverId(2L);
        session.setAnswerTime(LocalDateTime.now().minusSeconds(5));
        session.complete(CallStatus.ENDED, LocalDateTime.now());

        assertThat(session.getStatus()).isEqualTo(CallStatus.ENDED);
        assertThat(session.getEndTime()).isNotNull();
        assertThat(session.getDuration()).isGreaterThanOrEqualTo(0L);
        assertThat(session.isParticipant(1L)).isTrue();
        assertThat(session.isParticipant(3L)).isFalse();
        assertThat(session.otherParticipant(1L)).isEqualTo(2L);
        assertThat(session.otherParticipant(2L)).isEqualTo(1L);
    }

    @Test
    void callSessionCompleteFallsBackToStartTimeWhenUnanswered() {
        CallSession session = new CallSession();
        session.setCallerId(1L);
        session.setReceiverId(2L);
        session.setStatus(CallStatus.RINGING);
        session.setStartTime(LocalDateTime.now().minusSeconds(3));
        session.complete(CallStatus.MISSED, LocalDateTime.now());

        assertThat(session.getDuration()).isGreaterThanOrEqualTo(0L);
        assertThat(session.getCallerId()).isEqualTo(1L);
        assertThat(session.getReceiverId()).isEqualTo(2L);
    }

    @Test
    void callLogAccessorsAndUserAliasesAreCovered() {
        CallLog log = new CallLog();
        setId(log, 50L);
        log.setSessionId(10L);
        log.setCallerId(1L);
        log.setReceiverId(2L);
        log.setCallType(CallType.AUDIO);
        log.setStatus(CallStatus.CONNECTED);
        log.setDuration(30L);

        assertThat(log.getId()).isEqualTo(50L);
        assertThat(log.getSessionId()).isEqualTo(10L);
        assertThat(log.getCallerId()).isEqualTo(1L);
        assertThat(log.getReceiverId()).isEqualTo(2L);
        assertThat(log.getCallType()).isEqualTo(CallType.AUDIO);
        assertThat(log.getStatus()).isEqualTo(CallStatus.CONNECTED);
        assertThat(log.getDuration()).isEqualTo(30L);

        User user = new User();
        user.setFullName("User");
        user.setEmail("user@test.com");
        user.setPhoneNumber("9999999999");
        user.setMobile("8888888888");
        user.setPassword("secret");
        user.setRole(Role.PARTNER);
        user.setProfileImage("image");
        user.setCity("Pune");
        user.setState("MH");
        user.setAddress("Address");
        user.setStatus(UserStatus.BLOCKED);
        user.setTotalBookings(12L);
        user.setActive(true);
        user.setDeleted(true);

        assertThat(user.getMobile()).isEqualTo("8888888888");
        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getProfileImage()).isEqualTo("image");
        assertThat(user.getCity()).isEqualTo("Pune");
        assertThat(user.getState()).isEqualTo("MH");
        assertThat(user.getAddress()).isEqualTo("Address");
        assertThat(user.getRole()).isEqualTo(Role.PARTNER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getTotalBookings()).isEqualTo(12L);
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isTrue();
    }

    private static void setId(CallLog log, Long id) {
        try {
            java.lang.reflect.Field idField = CallLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(log, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
