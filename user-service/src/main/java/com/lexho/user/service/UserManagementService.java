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
import java.util.List;
import java.util.Locale;

@Service
public class UserManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementService.class);

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
                Math.min(Math.max(size, 1), 100),
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
                String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("phoneNumber")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (normalizedName != null) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + normalizedName.toLowerCase(Locale.ROOT) + "%"));
            }
            if (normalizedPhone != null) {
                predicates.add(cb.like(cb.lower(root.get("phoneNumber")), "%" + normalizedPhone.toLowerCase(Locale.ROOT) + "%"));
            }
            if (normalizedEmail != null) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + normalizedEmail.toLowerCase(Locale.ROOT) + "%"));
            }
            if (roleFilter != null) {
                predicates.add(cb.equal(root.get("role"), roleFilter));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (normalizedCity != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + normalizedCity.toLowerCase(Locale.ROOT) + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<AdminUserResponse> responseData = users.getContent().stream()
                .map(this::toUserListResponse)
                .toList();

        return new PageResponse<>(responseData, users.getTotalElements(), users.getNumber(), users.getSize());
    }

    @Transactional
    public AdminUserResponse addUser(AdminUserCreateRequest request) {
        LOG.info("Admin creating user with phone={} role={}", request.getPhoneNumber(), request.getRole());
        validateCreateRequest(request);

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalize(request.getEmail()));
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(parseRoleForCreateOrUpdate(request.getRole()));
        user.setProfileImage(normalize(request.getProfileImage()));
        user.setCity(normalize(request.getCity()));
        user.setState(normalize(request.getState()));
        user.setAddress(normalize(request.getAddress()));
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

        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setAddress(normalize(request.getAddress()));
        user.setRole(parseRoleForCreateOrUpdate(request.getRole()));
        user.setCity(normalize(request.getCity()));
        user.setState(normalize(request.getState()));
        user.setProfileImage(normalize(request.getProfileImage()));

        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse blockUser(Long id) {
        LOG.info("Admin blocking user id={}", id);
        User user = getActiveUser(id);
        user.setStatus(UserStatus.BLOCKED);
        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse unblockUser(Long id) {
        LOG.info("Admin unblocking user id={}", id);
        User user = getActiveUser(id);
        user.setStatus(UserStatus.ACTIVE);
        return mapper.toResponse(userRepository.save(user));
    }

    public AdminUserDetailResponse getUserDetails(Long id) {
        User user = getActiveUser(id);
        BookingSummaryDto summary = fetchBookingSummary(id, user.getTotalBookings() == null ? 0L : user.getTotalBookings());
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
        BookingSummaryDto summary = fetchBookingSummary(user.getId(), user.getTotalBookings() == null ? 0L : user.getTotalBookings());
        if (summary.getTotalBookings() != user.getTotalBookings()) {
            user.setTotalBookings(summary.getTotalBookings());
            userRepository.save(user);
        }
        AdminUserResponse response = mapper.toResponse(user);
        response.setTotalBookings(summary.getTotalBookings());
        return response;
    }

    private BookingSummaryDto fetchBookingSummary(Long userId, long fallbackTotal) {
        BookingSummaryDto summary = new BookingSummaryDto();
        try {
            BookingSummaryClientResponse response = bookingServiceClient.getUserBookingSummary(userId);
            summary.setTotalBookings(response.getTotalBookings());
            summary.setCompletedBookings(response.getCompletedBookings());
            summary.setCancelledBookings(response.getCancelledBookings());
            return summary;
        } catch (Exception ex) {
            LOG.warn("Failed to fetch booking summary from booking-service for userId={}. Using fallback.", userId);
            summary.setTotalBookings(fallbackTotal);
            summary.setCompletedBookings(0L);
            summary.setCancelledBookings(0L);
            return summary;
        }
    }

    private User getActiveUser(Long id) {
        return userRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validateCreateRequest(AdminUserCreateRequest request) {
        if (userRepository.existsByPhoneNumberAndDeletedFalse(request.getPhoneNumber().trim())) {
            throw new UserAlreadyExistsException("User already exists with phone number: " + request.getPhoneNumber());
        }
        String email = normalize(request.getEmail());
        if (email != null && userRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new UserAlreadyExistsException("User already exists with email: " + email);
        }
        parseRoleForCreateOrUpdate(request.getRole());
    }

    private void validateUpdateRequest(Long id, AdminUserUpdateRequest request) {
        if (userRepository.existsByPhoneNumberAndDeletedFalseAndIdNot(request.getPhoneNumber().trim(), id)) {
            throw new UserAlreadyExistsException("Another user already exists with phone number: " + request.getPhoneNumber());
        }
        parseRoleForCreateOrUpdate(request.getRole());
    }

    private Role parseRoleForCreateOrUpdate(String roleText) {
        String normalized = normalize(roleText);
        if (normalized == null) {
            throw new InvalidRoleException(String.valueOf(roleText));
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("CUSTOMER".equals(upper)) {
            return Role.CUSTOMER;
        }
        if ("PARTNER".equals(upper) || "PROVIDER".equals(upper)) {
            return Role.PARTNER;
        }
        throw new InvalidRoleException(roleText);
    }

    private Role parseRoleFilter(String roleText) {
        String normalized = normalize(roleText);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CUSTOMER" -> Role.CUSTOMER;
            case "PARTNER", "PROVIDER" -> Role.PARTNER;
            case "ADMIN" -> Role.ADMIN;
            case "SUPER_ADMIN" -> Role.ADMIN;
            default -> throw new InvalidRoleException(roleText);
        };
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
        if (normalized == null) {
            return "createdAt";
        }
        return switch (normalized) {
            case "fullName", "email", "phoneNumber", "city", "role", "status", "totalBookings", "createdAt", "updatedAt" -> normalized;
            default -> "createdAt";
        };
    }
}
