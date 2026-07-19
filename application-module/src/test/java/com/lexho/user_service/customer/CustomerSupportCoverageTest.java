package com.lexho.user_service.customer;

import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.customer.controller.CustomerCartFallbackController;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.location.config.LocationAsyncConfig;
import com.lexho.user_service.customer.location.controller.HealthController;
import com.lexho.user_service.customer.location.controller.RootController;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeRequest;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.entity.AddressType;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.entity.LocationAuditLog;
import com.lexho.user_service.customer.location.entity.ServiceablePincode;
import com.lexho.user_service.customer.location.exception.InvalidCoordinatesException;
import com.lexho.user_service.customer.location.exception.ServiceabilityException;
import com.lexho.user_service.customer.location.service.ReverseGeocodePipelineService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.service.CustomerCartService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.dto.ApiResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerSupportCoverageTest {

    @Test
    void cartFallbackControllerDelegatesAllEndpoints() {
        CustomerCartService customerCartService = Mockito.mock(CustomerCartService.class);
        CustomerCartSlotService customerCartSlotService = Mockito.mock(CustomerCartSlotService.class);
        CustomerCartFallbackController controller = new CustomerCartFallbackController(customerCartService, customerCartSlotService);

        CustomerCartResponse cartResponse = CustomerCartResponse.builder()
                .cartId(1L)
                .address(CustomerCartResponse.Address.builder().id(2L).fullAddress("Addr").city("City").pincode("123").build())
                .build();
        CartConfigResponse configResponse = new CartConfigResponse();
        configResponse.setMaxCartItems(5);
        CustomerUpdateCartItemRequest update = new CustomerUpdateCartItemRequest();
        update.setItemId(10L);
        update.setQuantity(2);

        when(customerCartService.getCart(8L)).thenReturn(cartResponse);
        when(customerCartService.config()).thenReturn(configResponse);
        when(customerCartService.updateQuantity(8L, update)).thenReturn(cartResponse);
        when(customerCartSlotService.slotsForDate(java.time.LocalDate.of(2026, 6, 20))).thenReturn(Map.of("count", 4));

        assertThat(controller.getCart(8L).getData()).isSameAs(cartResponse);
        assertThat(controller.cartConfig().getData()).isSameAs(configResponse);
        assertThat(controller.updateCartItem(8L, update).getData()).isSameAs(cartResponse);
        assertThat(controller.removeCartItem(8L, 10L).isSuccess()).isTrue();
        assertThat(controller.clearCart(8L).isSuccess()).isTrue();
        assertThat(controller.updateCartMeta(8L, Map.of("x", 1)).getData()).isSameAs(cartResponse);
        assertThat(controller.cartSlots(java.time.LocalDate.of(2026, 6, 20)).getData()).containsEntry("count", 4);

        verify(customerCartService).removeItem(8L, 10L);
        verify(customerCartService).clearCart(8L);
    }

    @Test
    void rootAndHealthControllersReturnExpectedPayloads() {
        RootController rootController = new RootController();
        HealthController healthController = new HealthController();

        assertThat(rootController.root().getHeaders().getLocation()).hasToString("/api/health");
        assertThat(rootController.ping()).containsEntry("ok", true).containsEntry("service", "lexho-user-service");
        assertThat(healthController.health()).containsEntry("status", "UP");
        assertThat(healthController.ping()).containsEntry("ok", true);
    }

    @Test
    void locationSupportClassesAndEntitiesCoverLifecycleAndAccessors() throws Exception {
        ReverseGeocodeService reverseGeocodeService = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = new ReverseGeocodePipelineService(reverseGeocodeService);
        ReverseGeocodeResponse reverseGeocodeResponse = new ReverseGeocodeResponse();
        reverseGeocodeResponse.city = "Delhi";
        when(reverseGeocodeService.reverse(12.3, 45.6)).thenReturn(reverseGeocodeResponse);

        CompletableFuture<ReverseGeocodeResponse> future = pipelineService.reverseAsync(12.3, 45.6);
        assertThat(future.join().city).isEqualTo("Delhi");

        Executor executor = new LocationAsyncConfig().locationTaskExecutor();
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        assertThat(((ThreadPoolTaskExecutor) executor).getThreadNamePrefix()).isEqualTo("location-async-");

        ReverseGeocodeRequest request = new ReverseGeocodeRequest();
        request.latitude = 12.3;
        request.longitude = 77.6;
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(request)).isEmpty();

        ServiceablePincode pincode = new ServiceablePincode();
        setField(pincode, "id", "id-1");
        pincode.setPincode("110001");
        pincode.setCity("Delhi");
        pincode.setActive(true);
        assertThat(pincode.getId()).isEqualTo("id-1");
        assertThat(pincode.getPincode()).isEqualTo("110001");
        assertThat(pincode.getCity()).isEqualTo("Delhi");
        assertThat(pincode.isActive()).isTrue();

        CustomerCurrentLocation currentLocation = new CustomerCurrentLocation();
        currentLocation.setCustomerId(9L);
        currentLocation.setLatitude(BigDecimal.ONE);
        currentLocation.setLongitude(BigDecimal.TEN);
        currentLocation.setFormattedAddress("Addr");
        currentLocation.setCity("City");
        currentLocation.setState("State");
        currentLocation.setPincode("123");
        currentLocation.setServiceAvailable(true);
        currentLocation.setManualConfirmed(true);
        currentLocation.setAccuracyMeters(BigDecimal.valueOf(2.2));
        currentLocation.setConfidenceScore(BigDecimal.valueOf(0.95));
        currentLocation.setLocationAgeSeconds(15);
        currentLocation.setDetectionSource("GPS");
        currentLocation.setMockLocation(true);
        currentLocation.setSuspiciousJump(true);
        currentLocation.setSpoofAttemptCount(3);
        currentLocation.setRiskScore(BigDecimal.valueOf(7.5));
        currentLocation.setLastIdempotencyKey("key");
        currentLocation.setLastLocationFingerprint("fp");
        assertThat(currentLocation.getCustomerId()).isEqualTo(9L);
        assertThat(currentLocation.isMockLocation()).isTrue();
        assertThat(currentLocation.getLastLocationFingerprint()).isEqualTo("fp");

        CustomerAddress address = new CustomerAddress();
        address.setCustomerId(4L);
        address.setType(AddressType.HOME);
        address.setAddressLine("Street");
        address.setCity("City");
        address.setState("State");
        address.setPincode("123");
        address.defaults();
        invokeMethod(address, "onCreate");
        assertThat(address.getCountry()).isEqualTo("India");
        assertThat(address.getCreatedAt()).isNotNull();
        address.setUpdatedBy("tester");
        invokeMethod(address, "onUpdate");
        assertThat(address.getUpdatedBy()).isEqualTo("tester");

        LocationAuditLog log = new LocationAuditLog();
        log.setCustomerId(1L);
        log.setLatitude(BigDecimal.ONE);
        log.setLongitude(BigDecimal.TEN);
        log.setPincode("123");
        log.setSource("GPS");
        log.setMockLocation(true);
        log.setSuspiciousJump(true);
        log.setRiskScore(BigDecimal.ONE);
        log.setEventType("UPDATE");
        log.setEventReason("manual");
        log.prePersist();

        assertThat(new ServiceabilityException("bad")).hasMessage("bad");
        assertThat(new InvalidCoordinatesException("bad")).hasMessage("bad");
    }

    @Test
    void customerCartResponseAddressBuilderAndApiResponseHelpersWork() {
        CustomerCartResponse.Address address = CustomerCartResponse.Address.builder()
                .id(1L)
                .fullAddress("Full")
                .city("City")
                .pincode("110001")
                .build();
        CustomerCartResponse response = CustomerCartResponse.builder().cartId(9L).address(address).build();
        ApiResponse<CustomerCartResponse> success = ApiResponse.success("ok", response);

        assertThat(response.getAddress().getFullAddress()).isEqualTo("Full");
        assertThat(success.getData().getCartId()).isEqualTo(9L);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void invokeMethod(Object target, String methodName) throws Exception {
        var method = target.getClass().getSuperclass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}
