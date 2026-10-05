package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.lexhopartner.entity.PartnerAvailability;
import com.lexho.user_service.lexhopartner.entity.PartnerDeviceToken;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = PartnerRepositoryClusterTest.JpaConfig.class)
class PartnerRepositoryClusterTest {

    @Autowired
    private PartnerOperationalProfileRepository profileRepository;

    @Autowired
    private PartnerServiceAreaRepository serviceAreaRepository;

    @Autowired
    private PartnerDeviceTokenRepository deviceTokenRepository;

    @Autowired
    private PartnerAvailabilityRepository availabilityRepository;

    @Test
    void partnerRepositoriesPersistProfileTokensAreasAndAvailability() {
        PartnerOperationalProfile partner = profileRepository.save(partner(7001L, "9999990001", "alice@example.com"));

        serviceAreaRepository.save(area(partner.getId(), "110001"));
        serviceAreaRepository.save(area(partner.getId(), "110002"));
        PartnerDeviceToken token = deviceTokenRepository.save(token(partner.getId(), "token-1"));
        availabilityRepository.save(availability(partner.getId(), true));

        assertThat(serviceAreaRepository.findByPartnerIdOrderByPincodeAsc(partner.getId()))
                .extracting(PartnerServiceArea::getPincode)
                .containsExactly("110001", "110002");
        assertThat(deviceTokenRepository.findByPartnerIdAndToken(partner.getId(), "token-1")).contains(token);
        assertThat(availabilityRepository.findById(partner.getId()))
                .hasValueSatisfying(found -> assertThat(found.isOnline()).isTrue());

        serviceAreaRepository.deleteByPartnerId(partner.getId());
        assertThat(serviceAreaRepository.findByPartnerIdOrderByPincodeAsc(partner.getId())).isEmpty();
    }

    @Test
    void partnerProfileEnforcesUniqueMobileNumbers() {
        profileRepository.saveAndFlush(partner(8001L, "9999990009", "one@example.com"));

        assertThatThrownBy(() -> profileRepository.saveAndFlush(partner(8002L, "9999990009", "two@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PartnerOperationalProfile partner(Long id, String mobileNumber, String email) {
        PartnerOperationalProfile partner = new PartnerOperationalProfile();
        partner.setId(id);
        partner.setName("Partner " + id);
        partner.setMobileNumber(mobileNumber);
        partner.setServiceCategory("Cleaning");
        partner.setRating(4.8);
        partner.setTotalReviews(12L);
        partner.setOnline(true);
        partner.setVerified(true);
        partner.setAccountNumber("123456789");
        partner.setIfscCode("IFSC0001");
        partner.setFullName("Partner " + id);
        partner.setPhoneNumber(mobileNumber);
        partner.setDeleted(false);
        partner.setExperienceYears(5);
        partner.setPartnerOperationalStatus(PartnerOperationalStatus.APPROVED);
        partner.setServiceCategoryId(10L);
        partner.setRiskStatus("NORMAL");
        partner.setFraudFlag(false);
        partner.setBankVerificationStatus("VERIFIED");
        return partner;
    }

    private PartnerServiceArea area(Long partnerId, String pincode) {
        PartnerServiceArea area = new PartnerServiceArea();
        area.setPartnerId(partnerId);
        area.setPincode(pincode);
        area.setCity("Delhi");
        return area;
    }

    private PartnerDeviceToken token(Long partnerId, String value) {
        PartnerDeviceToken token = new PartnerDeviceToken();
        token.setPartnerId(partnerId);
        token.setToken(value);
        token.setDeviceType("ANDROID");
        token.setActive(true);
        return token;
    }

    private PartnerAvailability availability(Long partnerId, boolean online) {
        PartnerAvailability availability = new PartnerAvailability();
        availability.setPartnerId(partnerId);
        availability.setOnline(online);
        return availability;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            PartnerOperationalProfile.class,
            PartnerServiceArea.class,
            PartnerDeviceToken.class,
            PartnerAvailability.class
    })
    @EnableJpaRepositories(
            basePackageClasses = {
                    PartnerOperationalProfileRepository.class,
                    PartnerServiceAreaRepository.class,
                    PartnerDeviceTokenRepository.class,
                    PartnerAvailabilityRepository.class
            },
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerOperationalProfileRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerServiceAreaRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerDeviceTokenRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartnerAvailabilityRepository.class)
            }
    )
    static class JpaConfig {
    }
}

