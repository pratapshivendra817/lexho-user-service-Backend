package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.client.BookingServiceClient;
import com.lexho.user.dto.*;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.mapper.UserManagementMapper;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BookingServiceClient bookingServiceClient;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(
                userRepository,
                new UserManagementMapper(),
                passwordEncoder,
                bookingServiceClient
        );
    }

    @Test
    void addUserShouldCreateProvider() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("Test Partner");
        request.setEmail("partner@test.com");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setRole("PARTNER");

        when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = userManagementService.addUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(Role.PARTNER, saved.getRole());
        assertEquals("encoded-pass", saved.getPassword());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals("PARTNER", response.getRole());
    }

    @Test
    void addUserShouldRejectInvalidRole() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("Invalid Role");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setRole("MANAGER");

        assertThrows(InvalidRoleException.class, () -> userManagementService.addUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void blockUserShouldSetBlockedStatus() {
        User user = new User("9876543210", true);
        user.setFullName("Test User");
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);

        when(userRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = userManagementService.blockUser(10L);

        assertEquals("BLOCKED", response.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void getUserDetailsShouldIncludeFeignBookingSummary() {
        User user = new User("9876543210", true);
        user.setFullName("Test User");
        user.setRole(Role.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        user.setTotalBookings(2L);

        BookingSummaryClientResponse summary = new BookingSummaryClientResponse();
        summary.setTotalBookings(5L);
        summary.setCompletedBookings(3L);
        summary.setCancelledBookings(1L);

        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(user));
        when(bookingServiceClient.getUserBookingSummary(1L)).thenReturn(summary);

        AdminUserDetailResponse response = userManagementService.getUserDetails(1L);

        assertNotNull(response.getBookingSummary());
        assertEquals(5L, response.getBookingSummary().getTotalBookings());
        assertEquals(3L, response.getBookingSummary().getCompletedBookings());
        assertEquals(1L, response.getBookingSummary().getCancelledBookings());
    }
}
