package com.lexho.user.controller;

import com.lexho.common.enums.Role;
import com.lexho.user.entity.CallSession;
import com.lexho.user.entity.User;
import com.lexho.user.enums.CallStatus;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.repository.CallSessionRepository;
import com.lexho.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "socket.io.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CallControllerIntegrationTest {

    private static final String JWT_SECRET = "12345678901234567890123456789012";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CallSessionRepository callSessionRepository;

    private User caller;
    private User receiver;
    private String callerToken;

    @BeforeEach
    void setUp() {
        callSessionRepository.deleteAll();
        userRepository.deleteAll();
        caller = userRepository.save(activeUser("Caller", "9000000001", Role.CUSTOMER));
        receiver = userRepository.save(activeUser("Receiver", "9000000002", Role.PARTNER));
        callerToken = tokenFor(caller.getId(), caller.getPhoneNumber(), caller.getRole());
    }

    @Test
    void historyEndpointReturnsOwnedSessions() throws Exception {
        callSessionRepository.save(session(caller.getId(), receiver.getId(), CallStatus.ENDED));

        mockMvc.perform(get("/calls/history")
                        .header("Authorization", "Bearer " + callerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].callerId").value(caller.getId()));
    }

    @Test
    void getCallEndpointEnforcesOwnership() throws Exception {
        CallSession session = callSessionRepository.save(session(caller.getId(), receiver.getId(), CallStatus.ENDED));

        mockMvc.perform(get("/calls/{id}", session.getId())
                        .header("Authorization", "Bearer " + callerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(session.getId()));
    }

    @Test
    void missedEndpointReturnsMissedCalls() throws Exception {
        callSessionRepository.save(session(receiver.getId(), caller.getId(), CallStatus.MISSED));

        mockMvc.perform(get("/calls/missed")
                        .header("Authorization", "Bearer " + callerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("MISSED"));
    }

    @Test
    void endEndpointClosesActiveCall() throws Exception {
        CallSession session = callSessionRepository.save(session(caller.getId(), receiver.getId(), CallStatus.CONNECTED));
        session.setAnswerTime(LocalDateTime.now().minusSeconds(5));
        session = callSessionRepository.save(session);

        mockMvc.perform(post("/calls/end")
                        .header("Authorization", "Bearer " + callerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":" + session.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    private static User activeUser(String name, String mobile, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setPhoneNumber(mobile);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setTotalBookings(0L);
        user.setDeleted(false);
        return user;
    }

    private static CallSession session(Long callerId, Long receiverId, CallStatus status) {
        CallSession session = new CallSession();
        session.setCallerId(callerId);
        session.setReceiverId(receiverId);
        session.setStatus(status);
        session.setStartTime(LocalDateTime.now().minusMinutes(1));
        session.setDuration(0L);
        if (!status.isActive()) {
            session.setEndTime(LocalDateTime.now());
        }
        return session;
    }

    private static String tokenFor(Long userId, String subject, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer("lexho-user-service")
                .addClaims(Map.of("userId", userId, "role", role.name(), "aud", "lexho-clients"))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3600_000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
}
