package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.client.BookingServiceClient;
import com.lexho.user.dto.AdminUserCreateRequest;
import com.lexho.user.dto.AdminUserDetailResponse;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.AdminUserUpdateRequest;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.exception.UserNotFoundException;
import com.lexho.user.mapper.UserManagementMapper;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
class UserManagementServiceAdditionalTest {

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
    void addUserNormalizesOptionalFieldsAndPersistsEncodedPassword() {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("  Test User  ");
        request.setEmail("  ");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setRole(" customer ");
        request.setProfileImage(" ");
        request.setCity("  Pune ");
        request.setState(" Maharashtra ");
        request.setAddress("  Address ");

        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = service.addUser(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Test User");
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getProfileImage()).isNull();
        assertThat(saved.getCity()).isEqualTo("Pune");
        assertThat(saved.getState()).isEqualTo("Maharashtra");
        assertThat(saved.getAddress()).isEqualTo("Address");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "manager", "admin"})
    void addUserRejectsInvalidCreateRoles(String role) {
        AdminUserCreateRequest request = new AdminUserCreateRequest();
        request.setFullName("User");
        request.setPhoneNumber("9876543210");
        request.setPassword("password123");
        request.setRole(role);

        when(userRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(false);

        assertThatThrownBy(() -> service.addUser(request))
                .isInstanceOf(InvalidRoleException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "ADMIN,ADMIN",
            "SUPER_ADMIN,ADMIN",
            "PROVIDER,PARTNER",
            "PARTNER,PARTNER",
            "CUSTOMER,CUSTOMER"
    })
    void getAllUsersAcceptsSupportedRoleFilters(String roleFilter, String expectedRole) {
        User user = activeUser(1L, Role.valueOf(expectedRole));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(bookingServiceClient.getUserBookingSummary(1L)).thenThrow(new RuntimeException("fallback"));

        var response = service.getAllUsers(0, 10, "createdAt", "desc", null, null, null, null, roleFilter, null, null);

        assertThat(response.getData()).hasSize(1);
    }

    @Test
    void getAllUsersUsesRequestedSortableFieldAndDescendingFallback() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeUser(2L, Role.CUSTOMER))));
        when(bookingServiceClient.getUserBookingSummary(2L)).thenThrow(new RuntimeException("fallback"));

        service.getAllUsers(0, 10, "fullName", "desc", null, null, null, null, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("fullName")).isNotNull();
    }

    @Test
    void blockUserAndGetUserDetailsCoverRepositoryInteractions() {
        User user = activeUser(5L, Role.CUSTOMER);
        user.setTotalBookings(null);

        BookingSummaryClientResponse summary = new BookingSummaryClientResponse();
        summary.setTotalBookings(9L);
        summary.setCompletedBookings(7L);
        summary.setCancelledBookings(2L);

        when(userRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingServiceClient.getUserBookingSummary(5L)).thenReturn(summary);

        AdminUserResponse blocked = service.blockUser(5L);
        AdminUserDetailResponse detail = service.getUserDetails(5L);

        assertThat(blocked.getStatus()).isEqualTo("BLOCKED");
        assertThat(detail.getBookingSummary().getTotalBookings()).isEqualTo(9L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRejectsMissingUser() {
        when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUser(99L, new AdminUserUpdateRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void softDeleteRejectsMissingUser() {
        when(userRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDeleteUser(100L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getAllUsersDoesNotPersistWhenBookingTotalsMatch() {
        User user = activeUser(6L, Role.CUSTOMER);
        user.setTotalBookings(4L);
        BookingSummaryClientResponse summary = new BookingSummaryClientResponse();
        summary.setTotalBookings(4L);
        summary.setCompletedBookings(3L);
        summary.setCancelledBookings(1L);

        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(bookingServiceClient.getUserBookingSummary(6L)).thenReturn(summary);

        service.getAllUsers(0, 10, "createdAt", "desc", null, null, null, null, null, "ACTIVE", null);

        verify(userRepository, never()).save(user);
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
