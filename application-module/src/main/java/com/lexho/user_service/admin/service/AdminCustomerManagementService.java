package com.lexho.user_service.admin.service;

import com.lexho.user_service.customer.profile.entity.CustomerAccountDeleteRequestEntity;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.repository.CustomerAccountDeleteRequestRepository;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.util.LexhoConstants;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminCustomerManagementService {

    private final CustomerRepository customerRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final CustomerAccountDeleteRequestRepository deleteRequestRepository;

    public AdminCustomerManagementService(CustomerRepository customerRepository,
                                          LexhoAddressRepository lexhoAddressRepository,
                                          CustomerAccountDeleteRequestRepository deleteRequestRepository) {
        this.customerRepository = customerRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.deleteRequestRepository = deleteRequestRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String search, Boolean active, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortByOrDefault(sortBy)).ascending() : Sort.by(sortByOrDefault(sortBy)).descending()
        );
        Page<CustomerEntity> result = customerRepository.search(normalize(search), active, pageable);
        return Map.of(
                "items", result.getContent().stream().map(this::toRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> byId(Long customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(LexhoConstants.CUSTOMER_NOT_FOUND));
        return toRow(customer);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long customerId, Boolean active) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(LexhoConstants.CUSTOMER_NOT_FOUND));
        customer.setActive(active);
        return toRow(customerRepository.save(customer));
    }

    @Transactional
    public Map<String, Object> profileGovernance(Long customerId,
                                                 Boolean profileEditLocked,
                                                 Boolean emailVerified,
                                                 Boolean phoneVerified,
                                                 Boolean profileImageApproved,
                                                 String adminNotes) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(LexhoConstants.CUSTOMER_NOT_FOUND));
        if (profileEditLocked != null) {
            customer.setProfileEditLocked(profileEditLocked);
        }
        if (emailVerified != null) {
            customer.setEmailVerified(emailVerified);
        }
        if (phoneVerified != null) {
            customer.setPhoneVerified(phoneVerified);
        }
        if (profileImageApproved != null) {
            customer.setProfileImageApproved(profileImageApproved);
        }
        if (adminNotes != null) {
            customer.setAdminNotes(adminNotes.trim());
        }
        return toRow(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listDeleteRequests(Boolean resolved, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(LexhoConstants.FIELD_CREATED_AT).descending());
        Page<CustomerAccountDeleteRequestEntity> result = resolved == null
                ? deleteRequestRepository.findAll(pageable)
                : deleteRequestRepository.findByResolved(resolved, pageable);
        return Map.of(
                "items", result.getContent().stream().map(this::toDeleteRequestRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @Transactional
    public Map<String, Object> resolveDeleteRequest(Long requestId, boolean approve, String reason, Long adminId) {
        CustomerAccountDeleteRequestEntity request = deleteRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Delete request not found"));
        if (request.isResolved()) {
            return toDeleteRequestRow(request);
        }

        request.setResolved(true);
        request.setResolvedAt(Instant.now());
        request.setResolvedByAdminId(adminId);
        request.setResolutionStatus(approve ? "APPROVED" : "REJECTED");
        request.setResolutionReason(reason == null ? null : reason.trim());
        deleteRequestRepository.save(request);

        if (approve) {
            CustomerEntity customer = customerRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(LexhoConstants.CUSTOMER_NOT_FOUND));
            customer.setActive(false);
            customer.setProfileEditLocked(true);
            customerRepository.save(customer);
        }
        return toDeleteRequestRow(request);
    }

    private Map<String, Object> toRow(CustomerEntity customer) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", customer.getId());
        row.put("fullName", customer.getFullName());
        row.put("email", customer.getEmail());
        row.put("phoneNumber", customer.getPhoneNumber());
        row.put("mobile", customer.getMobile());
        row.put("gender", customer.getGender() == null ? null : customer.getGender().name());
        row.put("profileImageUrl", customer.getProfileImageUrl());
        row.put("active", customer.getActive());
        row.put("profileEditLocked", customer.getProfileEditLocked());
        row.put("emailVerified", customer.getEmailVerified());
        row.put("phoneVerified", customer.getPhoneVerified());
        row.put("profileImageApproved", customer.getProfileImageApproved());
        row.put("adminNotes", customer.getAdminNotes());
        row.put("addressCount", lexhoAddressRepository.findByUserId(customer.getId()).size());
        row.put(LexhoConstants.FIELD_CREATED_AT, customer.getCreatedAt());
        row.put(LexhoConstants.FIELD_UPDATED_AT, customer.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toDeleteRequestRow(CustomerAccountDeleteRequestEntity request) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", request.getId());
        row.put("userId", request.getUserId());
        row.put("resolved", request.isResolved());
        row.put("resolutionStatus", request.getResolutionStatus());
        row.put("resolutionReason", request.getResolutionReason());
        row.put("resolvedByAdminId", request.getResolvedByAdminId());
        row.put("resolvedAt", request.getResolvedAt());
        row.put(LexhoConstants.FIELD_CREATED_AT, request.getCreatedAt());
        return row;
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }

    private String sortByOrDefault(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return LexhoConstants.FIELD_UPDATED_AT;
        }
        return switch (sortBy) {
            case "id", "fullName", "email", "phoneNumber", "mobile",
                    LexhoConstants.FIELD_CREATED_AT, LexhoConstants.FIELD_UPDATED_AT, "active" -> sortBy;
            default -> LexhoConstants.FIELD_UPDATED_AT;
        };
    }
}
