package com.lexho.user.controller;

import com.lexho.common.enums.Role;
import com.lexho.user.client.BookingServiceClient;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "socket.io.enabled=false",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.jwt.secret=12345678901234567890123456789012"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class AdminUserManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private BookingServiceClient bookingServiceClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(user("Alice Johnson", "9000000011", "alice@test.com", Role.CUSTOMER, "Pune"));
        userRepository.save(user("Bob O'Connor", "9000000012", "bob.oconnor@test.com", Role.PARTNER, "Mumbai"));

        BookingSummaryClientResponse summary = new BookingSummaryClientResponse();
        summary.setTotalBookings(0L);
        summary.setCompletedBookings(0L);
        summary.setCancelledBookings(0L);
        when(bookingServiceClient.getUserBookingSummary(anyLong())).thenReturn(summary);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchTreatsSqlMetaCharactersAsLiteralInput() throws Exception {
        long beforeCount = userRepository.count();

        mockMvc.perform(get("/api/admin/users")
                        .param("search", "' OR 1=1 --")
                        .param("name", "%' OR '1'='1")
                        .param("email", "x' UNION SELECT * FROM users --")
                        .param("sortBy", "createdAt desc, (select 1)")
                        .param("sortDir", "desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchStillSupportsLegitimateSpecialCharacters() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("search", "O'Connor")
                        .param("sortBy", "fullName")
                        .param("sortDir", "asc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fullName").value("Bob O'Connor"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private static User user(String fullName, String phone, String email, Role role, String city) {
        User user = new User();
        user.setFullName(fullName);
        user.setPhoneNumber(phone);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setTotalBookings(0L);
        user.setDeleted(false);
        return user;
    }
}
