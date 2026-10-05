package com.lexho.user_service.customer.location.repository;

import com.lexho.user_service.customer.location.entity.AddressType;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = AddressRepositoryTest.JpaConfig.class)
class AddressRepositoryTest {

    @Autowired
    private LexhoAddressRepository lexhoAddressRepository;

    @Autowired
    private CustomerAddressRepository customerAddressRepository;

    @Autowired
    private CustomerCurrentLocationRepository customerCurrentLocationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void repositoriesHandleSoftDeleteDefaultsSearchAndLocationLookup() {
        LexhoAddress home = lexhoAddressRepository.save(lexhoAddress(3001L, "12A", false, true));
        LexhoAddress deleted = lexhoAddressRepository.save(lexhoAddress(3001L, "99B", true, false));

        assertThat(lexhoAddressRepository.findByUserIdAndIsDeletedFalse(3001L))
                .extracting(LexhoAddress::getHouseNo)
                .containsExactly("12A");
        assertThat(lexhoAddressRepository.findByIdAndUserIdAndIsDeletedFalse(home.getId(), 3001L)).isPresent();
        assertThat(lexhoAddressRepository.findByIdAndUserIdAndIsDeletedFalse(deleted.getId(), 3001L)).isEmpty();
        assertThat(lexhoAddressRepository.findByUserIdAndIsDeletedFalseForUpdate(3001L)).hasSize(1);

        CustomerAddress homeAddress = customerAddressRepository.save(customerAddress(4001L, "Sector 21", true, false));
        CustomerAddress workAddress = customerAddressRepository.save(customerAddress(4001L, "Cyber Hub", false, false));
        customerAddressRepository.save(customerAddress(4001L, "Archived", false, true));

        assertThat(customerAddressRepository.existsByCustomerIdAndIsDeletedFalse(4001L)).isTrue();
        assertThat(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(homeAddress.getId(), 4001L)).isPresent();
        assertThat(customerAddressRepository.findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(4001L))
                .hasValueSatisfying(found -> assertThat(found.getBuildingName()).isEqualTo("Sector 21"));

        Page<CustomerAddress> search = customerAddressRepository.search(4001L, "cyber", PageRequest.of(0, 10));
        assertThat(search.getContent()).extracting(CustomerAddress::getBuildingName).containsExactly("Cyber Hub");

        assertThat(customerAddressRepository.findAllByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(4001L))
                .extracting(CustomerAddress::getBuildingName)
                .containsExactly("Sector 21", "Cyber Hub");

        customerAddressRepository.clearDefault(4001L);
        entityManager.flush();
        entityManager.clear();
        assertThat(customerAddressRepository.findAllByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(4001L))
                .allMatch(address -> !address.isDefault());

        CustomerCurrentLocation location = new CustomerCurrentLocation();
        location.setCustomerId(4001L);
        location.setLatitude(new BigDecimal("28.6139000"));
        location.setLongitude(new BigDecimal("77.2090000"));
        location.setFormattedAddress("Connaught Place");
        location.setCity("Delhi");
        location.setState("Delhi");
        location.setPincode("110001");
        location.setServiceAvailable(true);
        location.setManualConfirmed(false);
        location.setAccuracyMeters(new BigDecimal("12.50"));
        location.setConfidenceScore(new BigDecimal("0.9850"));
        location.setLocationAgeSeconds(40);
        location.setDetectionSource("GPS");
        location.setMockLocation(false);
        location.setSuspiciousJump(false);
        location.setSpoofAttemptCount(0);
        location.setRiskScore(new BigDecimal("1.25"));
        location.setLastIdempotencyKey("idem-1");
        location.setLastLocationFingerprint("fp-1");
        customerCurrentLocationRepository.save(location);

        assertThat(customerCurrentLocationRepository.findByCustomerId(4001L))
                .hasValueSatisfying(found -> assertThat(found.getFormattedAddress()).isEqualTo("Connaught Place"));
    }

    private LexhoAddress lexhoAddress(Long userId, String houseNo, boolean deleted, boolean isDefault) {
        LexhoAddress entity = new LexhoAddress();
        entity.setUserId(userId);
        entity.setHouseNo(houseNo);
        entity.setArea("Green Park");
        entity.setCity("Delhi");
        entity.setState("Delhi");
        entity.setPincode("110016");
        entity.setLatitude(new BigDecimal("28.5580000"));
        entity.setLongitude(new BigDecimal("77.2040000"));
        entity.setDeleted(deleted);
        entity.setDefault(isDefault);
        entity.setAddressResolved(true);
        return entity;
    }

    private CustomerAddress customerAddress(Long customerId, String buildingName, boolean isDefault, boolean deleted) {
        CustomerAddress entity = new CustomerAddress();
        entity.setCustomerId(customerId);
        entity.setType(AddressType.HOME);
        entity.setFlatNo("101");
        entity.setBuildingName(buildingName);
        entity.setAddressLine("Road 1");
        entity.setArea("South");
        entity.setSector(buildingName);
        entity.setCity("Delhi");
        entity.setState("Delhi");
        entity.setCountry("India");
        entity.setPincode("110001");
        entity.setLatitude(new BigDecimal("28.6139000"));
        entity.setLongitude(new BigDecimal("77.2090000"));
        entity.setMobile("9999999999");
        entity.setDefault(isDefault);
        entity.setDeleted(deleted);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            LexhoAddress.class,
            CustomerAddress.class,
            CustomerCurrentLocation.class
    })
    @EnableJpaRepositories(
            basePackageClasses = {
                    LexhoAddressRepository.class,
                    CustomerAddressRepository.class,
                    CustomerCurrentLocationRepository.class
            },
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LexhoAddressRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomerAddressRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CustomerCurrentLocationRepository.class)
            }
    )
    static class JpaConfig {
    }
}

