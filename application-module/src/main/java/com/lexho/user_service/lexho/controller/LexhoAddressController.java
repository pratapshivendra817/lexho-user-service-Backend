package com.lexho.user_service.lexho.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.lexho.dto.LexhoDtos;
import com.lexho.user_service.lexho.service.LexhoAddressService;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import com.lexho.user_service.service.DeprecationControlService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/address", "/api/v1/address"})
@Hidden
public class LexhoAddressController {

    private final LexhoAddressService addressService;
    private final DeprecationControlService deprecationControlService;
    private final DeprecatedApiUsageService deprecatedApiUsageService;

    public LexhoAddressController(LexhoAddressService addressService,
                                  DeprecationControlService deprecationControlService,
                                  DeprecatedApiUsageService deprecatedApiUsageService) {
        this.addressService = addressService;
        this.deprecationControlService = deprecationControlService;
        this.deprecatedApiUsageService = deprecatedApiUsageService;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<LexhoDtos.AddressResponse>> add(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LexhoDtos.AddressUpsertRequest request
    ) {
        deprecationControlService.ensureLegacyAddressModuleEnabled();
        deprecatedApiUsageService.record("address.legacy.add");
        return ResponseEntity.ok()
                .headers(deprecationHeaders())
                .body(ApiResponse.success("Address added (deprecated route)", addressService.addOrUpdate(userId, request)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<LexhoDtos.AddressResponse>>> list(@RequestAttribute("userId") Long userId) {
        deprecationControlService.ensureLegacyAddressModuleEnabled();
        deprecatedApiUsageService.record("address.legacy.list");
        return ResponseEntity.ok()
                .headers(deprecationHeaders())
                .body(ApiResponse.success("Address list fetched (deprecated route)", addressService.list(userId)));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<LexhoDtos.AddressResponse>> update(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LexhoDtos.AddressUpsertRequest request
    ) {
        deprecationControlService.ensureLegacyAddressModuleEnabled();
        deprecatedApiUsageService.record("address.legacy.update");
        return ResponseEntity.ok()
                .headers(deprecationHeaders())
                .body(ApiResponse.success("Address updated (deprecated route)", addressService.addOrUpdate(userId, request)));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LexhoDtos.IdRequest request
    ) {
        deprecationControlService.ensureLegacyAddressModuleEnabled();
        deprecatedApiUsageService.record("address.legacy.delete");
        addressService.delete(userId, request.id());
        return ResponseEntity.ok()
                .headers(deprecationHeaders())
                .body(ApiResponse.success("Address deleted (deprecated route)", null));
    }

    @PutMapping("/default")
    public ResponseEntity<ApiResponse<LexhoDtos.AddressResponse>> makeDefault(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody LexhoDtos.IdRequest request
    ) {
        deprecationControlService.ensureLegacyAddressModuleEnabled();
        deprecatedApiUsageService.record("address.legacy.default");
        return ResponseEntity.ok()
                .headers(deprecationHeaders())
                .body(ApiResponse.success("Default address updated (deprecated route)", addressService.makeDefault(userId, request.id())));
    }

    private HttpHeaders deprecationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Deprecated", "true");
        headers.add("X-API-Replacement", "/api/customer/location/**");
        return headers;
    }
}
