package com.lexho.user_service.customer.location.controller;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.PagedResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeRequest;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.SaveAddressRequest;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckRequest;
import com.lexho.user_service.customer.location.dto.UpdateAddressRequest;
import com.lexho.user_service.customer.location.service.AddressService;
import com.lexho.user_service.customer.location.service.LocationService;
import com.lexho.user_service.customer.location.service.ReverseGeocodeService;
import com.lexho.user_service.customer.location.service.ServiceabilityService;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/customer/location", "/api/v1/customer/location"})
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Location", description = "Customer location and address management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CustomerLocationController {
    private final LocationService locationService;
    private final AddressService addressService;
    private final ReverseGeocodeService reverseGeocodeService;
    private final ServiceabilityService serviceabilityService;

    public CustomerLocationController(LocationService locationService,
                                      AddressService addressService,
                                      ReverseGeocodeService reverseGeocodeService,
                                      ServiceabilityService serviceabilityService) {
        this.locationService = locationService;
        this.addressService = addressService;
        this.reverseGeocodeService = reverseGeocodeService;
        this.serviceabilityService = serviceabilityService;
    }

    @PostMapping("/update")
    @Operation(summary = "Update current location")
    public ResponseEntity<ApiResponse<LocationUpdateResponse>> update(@RequestAttribute("userId") Long customerId,
                                                                      @RequestAttribute("role") String role,
                                                                      @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                              required = true,
                                                                              content = @Content(schema = @Schema(implementation = LocationUpdateRequest.class),
                                                                                      examples = @ExampleObject(value = "{\"latitude\":28.6123,\"longitude\":77.2290}")))
                                                                      @Valid @RequestBody LocationUpdateRequest request) {
        assertCustomerRole(role);
        return ResponseEntity.ok(ApiResponse.success("Location updated successfully", locationService.updateCurrentLocation(customerId, request)));
    }

    @PostMapping("/save-address")
    @Operation(summary = "Save customer address")
    public ResponseEntity<ApiResponse<AddressResponse>> saveAddress(@RequestAttribute("userId") Long customerId,
                                                                    @RequestAttribute("role") String role,
                                                                    @Valid @RequestBody SaveAddressRequest request) {
        assertCustomerRole(role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address saved successfully", addressService.save(customerId, request)));
    }

    @PutMapping("/address/{id}")
    @Operation(summary = "Update customer address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@RequestAttribute("userId") Long customerId,
                                                                      @RequestAttribute("role") String role,
                                                                      @PathVariable UUID id,
                                                                      @Valid @RequestBody UpdateAddressRequest request) {
        assertCustomerRole(role);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", addressService.update(customerId, id, request)));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current location")
    public ResponseEntity<ApiResponse<LocationUpdateResponse>> current(@RequestAttribute("userId") Long customerId,
                                                                       @RequestAttribute("role") String role) {
        assertCustomerRole(role);
        return ResponseEntity.ok(ApiResponse.success("Current location fetched successfully", locationService.getCurrentLocation(customerId)));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all addresses with pagination and optional search")
    public ResponseEntity<ApiResponse<PagedResponse<AddressResponse>>> all(@RequestAttribute("userId") Long customerId,
                                                                           @RequestAttribute("role") String role,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size,
                                                                           @RequestParam(required = false) String search,
                                                                           @RequestParam(required = false) String query) {
        assertCustomerRole(role);
        String effectiveSearch = (search == null || search.isBlank()) ? query : search;
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched successfully", addressService.list(customerId, page, size, effectiveSearch)));
    }

    @GetMapping("/search")
    @Operation(summary = "Fallback search endpoint")
    public ResponseEntity<ApiResponse<PagedResponse<AddressResponse>>> search(@RequestAttribute("userId") Long customerId,
                                                                              @RequestAttribute("role") String role,
                                                                              @RequestParam String query,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        assertCustomerRole(role);
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched successfully", addressService.list(customerId, page, size, query)));
    }

    @PatchMapping("/default/{addressId}")
    @Operation(summary = "Set default address")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@RequestAttribute("userId") Long customerId,
                                                                   @RequestAttribute("role") String role,
                                                                   @PathVariable UUID addressId) {
        assertCustomerRole(role);
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully", addressService.setDefault(customerId, addressId)));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Soft delete address")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestAttribute("userId") Long customerId,
                                                    @RequestAttribute("role") String role,
                                                    @PathVariable UUID addressId) {
        assertCustomerRole(role);
        addressService.softDelete(customerId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PostMapping("/reverse-geocode")
    @Operation(summary = "Reverse geocode from coordinates")
    public ResponseEntity<ApiResponse<ReverseGeocodeResponse>> reverseGeocode(@Valid @RequestBody ReverseGeocodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reverse geocode successful", reverseGeocodeService.reverse(request.latitude, request.longitude)));
    }

    @PostMapping("/serviceability/check")
    @Operation(summary = "Check pincode serviceability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkServiceability(@Valid @RequestBody ServiceabilityCheckRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Serviceability checked", Map.of("serviceAvailable", serviceabilityService.isServiceable(request.pincode))));
    }

    private void assertCustomerRole(String role) {
        if (role == null || !"CUSTOMER".equalsIgnoreCase(role)) {
            throw new UnauthorizedException("Unauthorized access");
        }
    }
}
