package com.lexho.user_service.customer.profile.service;

import com.lexho.user_service.admin.entity.AdminProfileContentEntity;
import com.lexho.user_service.customer.profile.dto.CustomerProfileContentResponse;
import com.lexho.user_service.admin.repository.AdminProfileContentRepository;
import com.lexho.user_service.customer.profile.dto.CustomerProfileAddressResponse;
import com.lexho.user_service.customer.profile.dto.CustomerEmailUpdateRequest;
import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.customer.profile.dto.CustomerProfileUpdateRequest;
import com.lexho.user_service.customer.profile.dto.UpdateProfileRequest;
import com.lexho.user_service.customer.profile.entity.CustomerAccountDeleteRequestEntity;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.entity.CustomerGender;
import com.lexho.user_service.customer.profile.mapper.CustomerProfileMapper;
import com.lexho.user_service.customer.profile.repository.CustomerAccountDeleteRequestRepository;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.customer.profile.storage.ImageStorageService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CustomerProfileService {
    private static final Logger log = LoggerFactory.getLogger(CustomerProfileService.class);

    private final CustomerRepository customerRepository;
    private final LexhoUserProfileRepository legacyProfileRepository;
    private final LexhoAddressRepository addressRepository;
    private final CustomerAccountDeleteRequestRepository deleteRequestRepository;
    private final AdminProfileContentRepository adminProfileContentRepository;
    private final CustomerProfileMapper profileMapper;
    private final ImageStorageService imageStorageService;

    public CustomerProfileService(CustomerRepository customerRepository,
                                  LexhoUserProfileRepository legacyProfileRepository,
                                  LexhoAddressRepository addressRepository,
                                  CustomerAccountDeleteRequestRepository deleteRequestRepository,
                                  AdminProfileContentRepository adminProfileContentRepository,
                                  CustomerProfileMapper profileMapper,
                                  ImageStorageService imageStorageService) {
        this.customerRepository = customerRepository;
        this.legacyProfileRepository = legacyProfileRepository;
        this.addressRepository = addressRepository;
        this.deleteRequestRepository = deleteRequestRepository;
        this.adminProfileContentRepository = adminProfileContentRepository;
        this.profileMapper = profileMapper;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(Long userId) {
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional
    public CustomerProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        assertProfileEditable(profile);

        profile.setFullName(request.fullName == null ? null : request.fullName.trim());
        if (request.email != null && !request.email.trim().isBlank()) {
            String email = request.email.trim().toLowerCase();
            if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
                throw new IllegalArgumentException("Email already exists");
            }
            profile.setEmail(email);
        }
        if (request.gender != null && !request.gender.trim().isBlank()) {
            profile.setGender(CustomerGender.valueOf(request.gender.trim().toUpperCase()));
        }
        profile.setMobile(profile.getPhoneNumber());
        log.info("customer_profile_updated userId={} fullName={} gender={}", userId, profile.getFullName(), profile.getGender());
        customerRepository.save(profile);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional
    public CustomerProfileResponse updateProfileDetails(Long userId, CustomerProfileUpdateRequest request) {
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        assertProfileEditable(profile);
        String firstName = request.firstName == null ? "" : request.firstName.trim();
        String lastName = request.lastName == null ? "" : request.lastName.trim();
        String fullName = (firstName + " " + lastName).trim();
        profile.setFullName(fullName);

        if (request.gender != null && !request.gender.trim().isBlank()) {
            profile.setGender(CustomerGender.valueOf(request.gender.trim().toUpperCase()));
        }
        profile.setMobile(profile.getPhoneNumber());
        customerRepository.save(profile);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional
    public CustomerProfileResponse updateEmail(Long userId, CustomerEmailUpdateRequest request) {
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        assertProfileEditable(profile);
        String email = request.email == null ? null : request.email.trim().toLowerCase();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new IllegalArgumentException("Email already exists");
        }
        profile.setEmail(email);
        customerRepository.save(profile);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<CustomerProfileAddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toAddress).toList();
    }

    @Transactional
    public CustomerProfileResponse uploadProfileImage(Long userId, MultipartFile file) {
        log.info("customer_profile_image_upload_requested userId={} contentType={} size={} originalName={}",
                userId, file != null ? file.getContentType() : null, file != null ? file.getSize() : null, file != null ? file.getOriginalFilename() : null);
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        assertProfileEditable(profile);
        String existing = profile.getProfileImageUrl();
        String imageUrl = imageStorageService.upload(file);
        profile.setProfileImageUrl(imageUrl);
        customerRepository.save(profile);
        imageStorageService.delete(existing);
        log.info("customer_profile_image_uploaded userId={} imageUrl={}", userId, imageUrl);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional
    public CustomerProfileResponse deleteProfileImage(Long userId) {
        CustomerEntity profile = findOrBootstrapCustomer(userId);
        assertProfileEditable(profile);
        String existing = profile.getProfileImageUrl();
        profile.setProfileImageUrl(null);
        customerRepository.save(profile);
        imageStorageService.delete(existing);
        log.info("customer_profile_image_deleted userId={}", userId);
        return profileMapper.toResponse(profile, addressRepository.findByUserId(userId));
    }

    @Transactional
    public void requestAccountDelete(Long userId) {
        if (deleteRequestRepository.existsByUserIdAndResolvedFalse(userId)) {
            return;
        }
        CustomerAccountDeleteRequestEntity entity = new CustomerAccountDeleteRequestEntity();
        entity.setUserId(userId);
        entity.setResolved(false);
        entity.setResolutionStatus("PENDING");
        deleteRequestRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAccountDeleteStatus(Long userId) {
        boolean requested = deleteRequestRepository.existsByUserIdAndResolvedFalse(userId);
        return Map.of("requested", requested);
    }

    @Transactional(readOnly = true)
    public CustomerProfileContentResponse getProfileContent() {
        AdminProfileContentEntity entity = adminProfileContentRepository.findById(1L).orElseGet(() -> {
            AdminProfileContentEntity e = new AdminProfileContentEntity();
            e.setId(1L);
            e.setAboutUs("");
            e.setTermsOfService("");
            e.setPrivacyPolicy("");
            return e;
        });
        return toResponse(entity);
    }

    private CustomerProfileAddressResponse toAddress(LexhoAddress a) {
        CustomerProfileAddressResponse r = new CustomerProfileAddressResponse();
        r.id = a.getId();
        r.label = "Address";
        r.addressLine = (a.getHouseNo() == null ? "" : a.getHouseNo()) + ", " + (a.getArea() == null ? "" : a.getArea()) + ", " + (a.getCity() == null ? "" : a.getCity());
        r.latitude = a.getLatitude() == null ? null : a.getLatitude().doubleValue();
        r.longitude = a.getLongitude() == null ? null : a.getLongitude().doubleValue();
        r.isDefault = a.isDefault();
        return r;
    }

    private CustomerProfileContentResponse toResponse(AdminProfileContentEntity entity) {
        CustomerProfileContentResponse response = new CustomerProfileContentResponse();
        response.aboutUs = entity.getAboutUs();
        response.termsOfService = entity.getTermsOfService();
        response.privacyPolicy = entity.getPrivacyPolicy();
        return response;
    }

    private CustomerEntity findOrBootstrapCustomer(Long userId) {
        return customerRepository.findById(userId)
                .orElseGet(() -> bootstrapFromLegacyProfile(userId));
    }

    private CustomerEntity bootstrapFromLegacyProfile(Long userId) {
        LexhoUserProfile legacy = legacyProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        CustomerEntity customer = new CustomerEntity();
        customer.setId(legacy.getId());
        customer.setFullName((legacy.getName() == null || legacy.getName().isBlank()) ? "Lexho User" : legacy.getName().trim());
        customer.setEmail(legacy.getEmail());
        customer.setPhoneNumber(legacy.getMobile());
        customer.setMobile(legacy.getMobile());
        customer.setProfileImageUrl(legacy.getProfileImage());
        if (legacy.getGender() != null && !legacy.getGender().isBlank()) {
            try {
                customer.setGender(CustomerGender.valueOf(legacy.getGender().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                customer.setGender(null);
            }
        }
        return customerRepository.save(customer);
    }

    private void assertProfileEditable(CustomerEntity profile) {
        if (Boolean.TRUE.equals(profile.getProfileEditLocked())) {
            throw new IllegalArgumentException("Profile updates are temporarily locked by admin");
        }
        if (!Objects.equals(profile.getActive(), Boolean.TRUE)) {
            throw new IllegalArgumentException("Inactive customer profile cannot be updated");
        }
    }
}
