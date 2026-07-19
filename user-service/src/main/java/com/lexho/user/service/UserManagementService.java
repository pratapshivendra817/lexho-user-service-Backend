package com.lexho.user.service;

import com.lexho.common.enums.Role;
import com.lexho.user.client.BookingServiceClient;
import com.lexho.user.dto.AdminUserCreateRequest;
import com.lexho.user.dto.AdminUserDetailResponse;
import com.lexho.user.dto.AdminUserResponse;
import com.lexho.user.dto.AdminUserUpdateRequest;
import com.lexho.user.dto.BookingSummaryClientResponse;
import com.lexho.user.dto.BookingSummaryDto;
import com.lexho.user.dto.PageResponse;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.exception.UserAlreadyExistsException;
import com.lexho.user.exception.UserNotFoundException;
import com.lexho.user.mapper.UserManagementMapper;
import com.lexho.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementService.class);
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Collection<String> SORTABLE_FIELDS = List.of(
            "fullName", "email", "phoneNumber", "city", "role", "status", "totalBookings", "createdAt", "updatedAt"
    );

    private final UserRepository userRepository;
    private final UserManagementMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final BookingServiceClient bookingServiceClient;

    public UserManagementService(UserRepository userRepository,
                                 UserManagementMapper mapper,
                                 PasswordEncoder passwordEncoder,
                                 BookingServiceClient bookingServiceClient) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.bookingServiceClient = bookingServiceClient;
    }

    public PageResponse<AdminUserResponse> getAllUsers(int page,
                                                       int size,
                                                       String sortBy,
                                                       String sortDir,
                                                       String search,
                                                       String name,
                                                       String phone,
                                                       String email,
                                                       String role,
                                                       String status,
                                                       String city) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, normalizeSortBy(sortBy))
        );

        Role roleFilter = parseRoleFilter(role);
        UserStatus statusFilter = parseStatus(status);
        String normalizedSearch = normalize(search);
        String normalizedName = normalize(name);
        String normalizedPhone = normalize(phone);
        String normalizedEmail = normalize(email);
        String normalizedCity = normalize(city);

        Page<User> users = userRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (normalizedSearch != null) {
                String like = likePattern(normalizedSearch);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("phoneNumber")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            addLikePredicate(predicates, normalizedName, value -> cb.like(cb.lower(root.get("fullName")), value));
            addLikePredicate(predicates, normalizedPhone, value -> cb.like(cb.lower(root.get("phoneNumber")), value));
            addLikePredicate(predicates, normalizedEmail, value -> cb.like(cb.lower(root.get("email")), value));
            if (roleFilter != null) {
                predicates.add(cb.equal(root.get("role"), roleFilter));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            addLikePredicate(predicates, normalizedCity, value -> cb.like(cb.lower(root.get("city")), value));

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return new PageResponse<>(
                users.getContent().stream().map(this::toUserListResponse).toList(),
                users.getTotalElements(),
                users.getNumber(),
                users.getSize()
        );
    }

    @Transactional
    public AdminUserResponse addUser(AdminUserCreateRequest request) {
        LOG.info("Admin creating user with phone={} role={}", request.getPhoneNumber(), request.getRole());
        validateCreateRequest(request);

        User user = new User();
        applyCommonUserFields(
                user,
                request.getFullName(),
                request.getPhoneNumber(),
                request.getRole(),
                request.getProfileImage(),
                request.getCity(),
                request.getState(),
                request.getAddress()
        );
        user.setEmail(normalize(request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setTotalBookings(0L);
        user.setDeleted(false);

        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserUpdateRequest request) {
        LOG.info("Admin updating user id={}", id);
        User user = getActiveUser(id);

        validateUpdateRequest(id, request);

        applyCommonUserFields(
                user,
                request.getFullName(),
                request.getPhoneNumber(),
                request.getRole(),
                request.getProfileImage(),
                request.getCity(),
                request.getState(),
                request.getAddress()
        );

        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse blockUser(Long id) {
        LOG.info("Admin blocking user id={}", id);
        return updateUserStatus(id, UserStatus.BLOCKED);
    }

    @Transactional
    public AdminUserResponse unblockUser(Long id) {
        LOG.info("Admin unblocking user id={}", id);
        return updateUserStatus(id, UserStatus.ACTIVE);
    }

    public AdminUserDetailResponse getUserDetails(Long id) {
        User user = getActiveUser(id);
        BookingSummaryDto summary = fetchBookingSummary(id, totalBookingsOf(user));
        return mapper.toDetailResponse(user, summary);
    }

    @Transactional
    public void softDeleteUser(Long id) {
        LOG.info("Admin soft deleting user id={}", id);
        User user = getActiveUser(id);
        user.setDeleted(true);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    private AdminUserResponse toUserListResponse(User user) {
        BookingSummaryDto summary = fetchBookingSummary(user.getId(), totalBookingsOf(user));
        if (summary.getTotalBookings() != totalBookingsOf(user)) {
            user.setTotalBookings(summary.getTotalBookings());
            userRepository.save(user);
        }
        AdminUserResponse response = mapper.toResponse(user);
        response.setTotalBookings(summary.getTotalBookings());
        return response;
    }

    private BookingSummaryDto fetchBookingSummary(Long userId, long fallbackTotal) {
        try {
            BookingSummaryClientResponse response = bookingServiceClient.getUserBookingSummary(userId);
            return toBookingSummary(response.getTotalBookings(), response.getCompletedBookings(), response.getCancelledBookings());
        } catch (Exception ex) {
            LOG.warn("Failed to fetch booking summary from booking-service for userId={}. Using fallback.", userId);
            return toBookingSummary(fallbackTotal, 0L, 0L);
        }
    }

    private User getActiveUser(Long id) {
        return userRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validateCreateRequest(AdminUserCreateRequest request) {
        String phoneNumber = request.getPhoneNumber().trim();
        assertUnique(
                userRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber),
                "User already exists with phone number: " + request.getPhoneNumber()
        );
        String email = normalize(request.getEmail());
        assertUnique(email != null && userRepository.existsByEmailIgnoreCaseAndDeletedFalse(email),
                "User already exists with email: " + email);
        parseRoleForCreateOrUpdate(request.getRole());
    }

    private void validateUpdateRequest(Long id, AdminUserUpdateRequest request) {
        assertUnique(
                userRepository.existsByPhoneNumberAndDeletedFalseAndIdNot(request.getPhoneNumber().trim(), id),
                "Another user already exists with phone number: " + request.getPhoneNumber()
        );
        parseRoleForCreateOrUpdate(request.getRole());
    }

    private Role parseRoleForCreateOrUpdate(String roleText) {
        return parseRole(roleText, false);
    }

    private Role parseRoleFilter(String roleText) {
        return parseRole(roleText, true);
    }

    private UserStatus parseStatus(String statusText) {
        String normalized = normalize(statusText);
        if (normalized == null) {
            return null;
        }
        try {
            return UserStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid user status: " + statusText);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        return normalized != null && SORTABLE_FIELDS.contains(normalized) ? normalized : DEFAULT_SORT_BY;
    }

    private void applyCommonUserFields(User user,
                                       String fullName,
                                       String phoneNumber,
                                       String role,
                                       String profileImage,
                                       String city,
                                       String state,
                                       String address) {
        user.setFullName(fullName.trim());
        user.setPhoneNumber(phoneNumber.trim());
        user.setRole(parseRoleForCreateOrUpdate(role));
        user.setProfileImage(normalize(profileImage));
        user.setCity(normalize(city));
        user.setState(normalize(state));
        user.setAddress(normalize(address));
    }

    private AdminUserResponse updateUserStatus(Long id, UserStatus status) {
        User user = getActiveUser(id);
        user.setStatus(status);
        return mapper.toResponse(userRepository.save(user));
    }

    private BookingSummaryDto toBookingSummary(long totalBookings, long completedBookings, long cancelledBookings) {
        BookingSummaryDto summary = new BookingSummaryDto();
        summary.setTotalBookings(totalBookings);
        summary.setCompletedBookings(completedBookings);
        summary.setCancelledBookings(cancelledBookings);
        return summary;
    }

    private long totalBookingsOf(User user) {
        return user.getTotalBookings() == null ? 0L : user.getTotalBookings();
    }

    private void assertUnique(boolean exists, String message) {
        if (exists) {
            throw new UserAlreadyExistsException(message);
        }
    }

    private void addLikePredicate(List<Predicate> predicates,
                                  String normalizedValue,
                                  java.util.function.Function<String, Predicate> predicateFactory) {
        if (normalizedValue != null) {
            predicates.add(predicateFactory.apply(likePattern(normalizedValue)));
        }
    }

    private String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private Role parseRole(String roleText, boolean allowAdmin) {
        String normalized = normalize(roleText);
        if (normalized == null) {
            return nullIfAllowed(roleText, allowAdmin);
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "CUSTOMER" -> Role.CUSTOMER;
            case "PARTNER", "PROVIDER" -> Role.PARTNER;
            case "ADMIN", "SUPER_ADMIN" -> allowAdmin ? Role.ADMIN : invalidRole(roleText);
            default -> invalidRole(roleText);
        };
    }

    private Role nullIfAllowed(String roleText, boolean allowNull) {
        if (allowNull) {
            return null;
        }
        throw new InvalidRoleException(String.valueOf(roleText));
    }

    private Role invalidRole(String roleText) {
        throw new InvalidRoleException(roleText);
    }
}
