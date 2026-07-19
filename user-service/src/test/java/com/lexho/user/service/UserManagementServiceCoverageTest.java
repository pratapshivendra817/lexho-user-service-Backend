package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.client.BookingServiceClient;
import com.lexho.user.dto.AdminUserCreateRequest;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.AdminUserUpdateRequest;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user.dto.PageResponse;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.exception.UserAlreadyExistsException;
import com.lexho.user.exception.UserNotFoundException;
import com.lexho.user.mapper.UserManagementMapper;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private BookingServiceClient bookingServiceClient;

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(
                userRepository,
                new UserManagementMapper(),
                passwordEncoder,
                bookingServiceClient
        );
    }

    @Test
    void getAllUsersNormalizesPagingAndUpdatesBookingTotalsFromClient() {
        User user = activeUser(11L, Role.CUSTOMER);
        user.setFullName("Alice Walker");
        user.setEmail("alice@test.com");
        user.setCity("Pune");
        user.setTotalBookings(1L);

        BookingSummaryClientResponse summary = new BookingSummaryClientResponse();
        summary.setTotalBookings(4L);
        summary.setCompletedBookings(3L);
        summary.setCancelledBookings(1L);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(bookingServiceClient.getUserBookingSummary(11L)).thenReturn(summary);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PageResponse<AdminUserResponse> response = service.getAllUsers(
                -5, 500, "unsupported", "asc", " alice ", " Alice ", " 999 ", " alice@test.com ",
                " provider ", " active ", " pune "
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTotalBookings()).isEqualTo(4L);
        assertThat(user.getTotalBookings()).isEqualTo(4L);
        verify(userRepository).save(user);
    }

    @Test
    void getAllUsersFallsBackWhenBookingClientFails() {
        User user = activeUser(12L, Role.PARTNER);
        user.setTotalBookings(7L);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(bookingServiceClient.getUserBookingSummary(12L)).thenThrow(new RuntimeException("booking-service down"));

        PageResponse<AdminUserResponse> response = service.getAllUsers(
                0, 10, "createdAt", "desc", null, null, null, null, null, null, null
        );

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTotalBookings()).isEqualTo(7L);
        verify(userRepository, never()).save(user);
    }

    @Test
    void getAllUsersRejectsInvalidRoleFilter() {
        assertThatThrownBy(() -> service.getAllUsers(
                0, 10, "createdAt", "desc", null, null, null, null, "manager", null, null
        )).isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void getAllUsersRejectsInvalidStatusFilter() {
        assertThatThrownBy(() -> service.getAllUsers(
                0, 10, "createdAt", "desc", null, null, null, null, null, "paused", null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid user status");
    }

    @Test
    void addUserRejectsDuplicatePhoneAndEmail() {
        AdminUserCreateRequest request = createRequest();

        when(userRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> service.addUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("phone number");

        when(userRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCaseAndDeletedFalse("partner@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.addUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void updateUserNormalizesFieldsAndMapsProviderRoleToPartner() {
        User user = activeUser(21L, Role.CUSTOMER);
        user.setFullName("Before");
        user.setPhoneNumber("9999999999");

        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setFullName("  Updated User  ");
        request.setPhoneNumber("8888888888");
        request.setAddress("  Some address ");
        request.setRole("provider");
        request.setCity("  Bengaluru ");
        request.setState("  Karnataka ");
        request.setProfileImage("  http://image ");

        when(userRepository.findByIdAndDeletedFalse(21L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndDeletedFalseAndIdNot("8888888888", 21L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = service.updateUser(21L, request);

        assertThat(user.getFullName()).isEqualTo("Updated User");
        assertThat(user.getPhoneNumber()).isEqualTo("8888888888");
        assertThat(user.getRole()).isEqualTo(Role.PARTNER);
        assertThat(user.getCity()).isEqualTo("Bengaluru");
        assertThat(response.getRole()).isEqualTo("PARTNER");
    }

    @Test
    void updateUserRejectsDuplicatePhone() {
        User user = activeUser(22L, Role.CUSTOMER);
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setFullName("Updated User");
        request.setPhoneNumber("8888888888");
        request.setRole("CUSTOMER");

        when(userRepository.findByIdAndDeletedFalse(22L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndDeletedFalseAndIdNot("8888888888", 22L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateUser(22L, request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("phone number");
    }

    @Test
    void unblockAndSoftDeleteUpdatePersistentState() {
        User user = activeUser(23L, Role.CUSTOMER);
        user.setStatus(UserStatus.BLOCKED);

        when(userRepository.findByIdAndDeletedFalse(23L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse unblocked = service.unblockUser(23L);
        assertThat(unblocked.getStatus()).isEqualTo("ACTIVE");

        service.softDeleteUser(23L);
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void getUserDetailsThrowsWhenUserDoesNotExist() {
        when(userRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserDetails(404L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("404");
    }

    private static AdminUserCreateRequest createRequest() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("Test Partner");
        request.setEmail(" partner@test.com ");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setRole("PARTNER");
        return request;
    }

    private static User activeUser(Long id, Role role) {
        User user = new User();
        user.setFullName("User");
        user.setPhoneNumber("9999999999");
        user.setPassword("secret");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        user.setTotalBookings(0L);
        setId(user, id);
        return user;
    }

    private static void setId(User user, Long id) {
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
