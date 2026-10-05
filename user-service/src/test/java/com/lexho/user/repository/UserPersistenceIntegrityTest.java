package com.lexho.user.repository;

import com.lexho.common.enums.Role;
import com.lexho.user.entity.CallLog;
import com.lexho.user.entity.CallSession;
import com.lexho.user.entity.User;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.enums.CallType;
import com.lexho.user.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(UserPersistenceIntegrityTest.RollbackHarness.class)
class UserPersistenceIntegrityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CallSessionRepository callSessionRepository;

    @Autowired
    private CallLogRepository callLogRepository;

    @Autowired
    private RollbackHarness rollbackHarness;

    @Test
    void userRejectsDuplicatePhoneNumberAtDatabaseLevel() {
        userRepository.saveAndFlush(user("9999999999", "one@test.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("9999999999", "two@test.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userRejectsDuplicateEmailAtDatabaseLevel() {
        userRepository.saveAndFlush(user("8888888888", "dup@test.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(user("7777777777", "dup@test.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void callLogRejectsDuplicateSessionIdAndPreservesLookup() {
        CallSession session = callSessionRepository.saveAndFlush(session(41L, 42L));
        callLogRepository.saveAndFlush(log(session.getId(), 41L, 42L));

        assertThat(callLogRepository.findBySessionId(session.getId())).isPresent();
        assertThatThrownBy(() -> callLogRepository.saveAndFlush(log(session.getId(), 41L, 42L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void transactionRollbackRemovesSessionAndLogTogether() {
        long sessionsBefore = callSessionRepository.count();
        long logsBefore = callLogRepository.count();

        assertThatThrownBy(rollbackHarness::createSessionAndLogThenFail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rollback");

        assertThat(callSessionRepository.count()).isEqualTo(sessionsBefore);
        assertThat(callLogRepository.count()).isEqualTo(logsBefore);
    }

    private static User user(String phoneNumber, String email) {
        User user = new User();
        user.setFullName("Test User");
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setTotalBookings(0L);
        user.setDeleted(false);
        return user;
    }

    private static CallSession session(Long callerId, Long receiverId) {
        CallSession session = new CallSession();
        session.setCallerId(callerId);
        session.setReceiverId(receiverId);
        session.setStatus(CallStatus.CONNECTED);
        session.setDuration(0L);
        return session;
    }

    private static CallLog log(Long sessionId, Long callerId, Long receiverId) {
        CallLog log = new CallLog();
        log.setSessionId(sessionId);
        log.setCallerId(callerId);
        log.setReceiverId(receiverId);
        log.setCallType(CallType.AUDIO);
        log.setStatus(CallStatus.CONNECTED);
        log.setDuration(0L);
        return log;
    }

    @Component
    static class RollbackHarness {

        private final CallSessionRepository callSessionRepository;
        private final CallLogRepository callLogRepository;

        RollbackHarness(CallSessionRepository callSessionRepository,
                        CallLogRepository callLogRepository) {
            this.callSessionRepository = callSessionRepository;
            this.callLogRepository = callLogRepository;
        }

        @Transactional
        public void createSessionAndLogThenFail() {
            CallSession session = callSessionRepository.saveAndFlush(session(71L, 72L));
            callLogRepository.saveAndFlush(log(session.getId(), 71L, 72L));
            throw new IllegalStateException("trigger rollback");
        }
    }
}
