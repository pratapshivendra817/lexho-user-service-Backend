package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.cookfamilysize.service.CookFamilySizeService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.dto.CookCoverageItemResponse;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingRequest;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceRequest;
import com.lexho.user_service.subscription.dto.SubscriptionQuoteRequest;
import com.lexho.user_service.subscription.entity.CookSubscriptionPricing;
import com.lexho.user_service.subscription.entity.SubscriptionDurationPrice;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.repository.CookSubscriptionPricingRepository;
import com.lexho.user_service.subscription.repository.SubscriptionDurationPriceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionPricingServiceTest {

    private final SubscriptionDurationPriceRepository durationRepository = mock(SubscriptionDurationPriceRepository.class);
    private final CookSubscriptionPricingRepository cookRepository = mock(CookSubscriptionPricingRepository.class);
    private final AdminServiceEntityRepository serviceRepository = mock(AdminServiceEntityRepository.class);
    private final CookFamilySizeService cookFamilySizeService = mock(CookFamilySizeService.class);
    private final CookCoverageService cookCoverageService = mock(CookCoverageService.class);
    private final SubscriptionPricingService service = new SubscriptionPricingService(
            durationRepository, cookRepository, serviceRepository, cookFamilySizeService, cookCoverageService);

    @Test
    void pricesAndQuoteShouldFallbackToGlobalDurationPricing() {
        SubscriptionDurationPrice global = durationPrice(1L, null, "2.0", "300.00", true);
        when(durationRepository.findByServiceIdAndActiveTrueOrderByDurationHoursAsc(99L)).thenReturn(List.of());
        when(durationRepository.findByServiceIdIsNullAndActiveTrueOrderByDurationHoursAsc()).thenReturn(List.of(global));
        when(durationRepository.findByServiceIdAndDurationHoursAndActiveTrue(99L, new BigDecimal("2.0")))
                .thenReturn(Optional.empty());
        when(durationRepository.findByServiceIdIsNullAndDurationHoursAndActiveTrue(new BigDecimal("2.0")))
                .thenReturn(Optional.of(global));

        assertEquals(1, service.prices(99L).size());
        assertEquals(new BigDecimal("900.00"), service.quote(99L, new BigDecimal("2.0"), 3).totalPrice());
        assertThrows(SubscriptionValidationException.class, () -> service.quote(99L, new BigDecimal("2.0"), 0));
    }

    @Test
    void subscriptionQuoteShouldCalculateCleaningAndCookVisits() {
        SubscriptionDurationPrice cleaning = durationPrice(1L, 5L, "1.5", "200.00", true);
        CookSubscriptionPricing cook = cookPrice(2L, 6L, FamilySize.MEDIUM, "350.00", true);
        when(durationRepository.findByServiceIdAndDurationHoursAndActiveTrue(5L, new BigDecimal("1.5")))
                .thenReturn(Optional.of(cleaning));
        when(cookRepository.findByServiceIdAndFamilySizeAndActiveTrue(6L, FamilySize.MEDIUM))
                .thenReturn(Optional.of(cook));
        when(cookFamilySizeService.label(FamilySize.MEDIUM)).thenReturn("3-4 Members");

        var cleaningQuote = service.quote(new SubscriptionQuoteRequest(5L, "cleaning", null, new BigDecimal("1.5"),
                List.of("Monday", "WEDNESDAY"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)));
        var cookQuote = service.quote(new SubscriptionQuoteRequest(6L, "cook", FamilySize.MEDIUM, null,
                List.of("Tuesday", "Thursday"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)));

        assertEquals(2, cleaningQuote.visits());
        assertEquals(new BigDecimal("400.00"), cleaningQuote.totalPrice());
        assertEquals(2, cookQuote.visits());
        assertEquals(new BigDecimal("700.00"), cookQuote.totalPrice());
        assertThrows(SubscriptionValidationException.class, () -> service.quote(new SubscriptionQuoteRequest(
                5L, "cleaning", null, null, List.of("bad-day"), LocalDate.now(), LocalDate.now())));
    }

    @Test
    void adminCreateUpdateDeleteShouldValidateServiceAndSoftDelete() {
        when(serviceRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(new AdminServiceEntity()));
        when(durationRepository.save(any(SubscriptionDurationPrice.class))).thenAnswer(invocation -> {
            SubscriptionDurationPrice saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(cookRepository.save(any(CookSubscriptionPricing.class))).thenAnswer(invocation -> {
            CookSubscriptionPricing saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 200L);
            return saved;
        });
        SubscriptionDurationPrice existing = durationPrice(101L, 5L, "2.0", "300.00", true);
        CookSubscriptionPricing existingCook = cookPrice(201L, 5L, FamilySize.LARGE, "450.00", true);
        when(durationRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(cookRepository.findById(201L)).thenReturn(Optional.of(existingCook));

        assertEquals(100L, service.create(new SubscriptionDurationPriceRequest(
                5L, new BigDecimal("1.0"), new BigDecimal("150.00"), true)).getData().id());
        assertEquals(200L, service.createCook(new CookSubscriptionPricingRequest(
                5L, FamilySize.SMALL, new BigDecimal("250.00"), true)).getData().id());
        assertEquals(new BigDecimal("350.00"), service.update(101L, new SubscriptionDurationPriceRequest(
                5L, new BigDecimal("2.0"), new BigDecimal("350.00"), true)).getData().perVisitPrice());
        assertEquals("Cook subscription price deleted successfully", service.deleteCook(201L).getMessage());

        when(serviceRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(new SubscriptionDurationPriceRequest(
                404L, new BigDecimal("1.0"), new BigDecimal("100.00"), true)));
    }

    @Test
    void cookCoverageShouldUseAdminManagedCoverageAndDefaultToMedium() {
        when(cookFamilySizeService.label(FamilySize.MEDIUM)).thenReturn("3-4 Members");
        when(cookFamilySizeService.label(FamilySize.LARGE)).thenReturn("5-6 Members");
        when(cookCoverageService.customerList(null, FamilySize.MEDIUM)).thenReturn(List.of(coverage(FamilySize.MEDIUM, "Paneer")));
        when(cookCoverageService.customerList(44L, FamilySize.LARGE)).thenReturn(List.of(coverage(FamilySize.LARGE, "Paneer")));

        assertEquals("MEDIUM", service.cookCoverage(null, null).familySize());
        assertEquals("Paneer", service.cookCoverage(44L, FamilySize.LARGE).items().get(0).name());
    }

    @Test
    void cookCoverageShouldNotAutoFallbackToGlobalWhenServiceSpecificDataMissing() {
        when(cookFamilySizeService.label(FamilySize.SMALL)).thenReturn("1-2 Members");
        when(cookCoverageService.customerList(123L, FamilySize.SMALL)).thenReturn(List.of());

        var response = service.cookCoverage(123L, FamilySize.SMALL);

        assertEquals(123L, response.serviceId());
        assertTrue(response.items().isEmpty());
    }

    @Test
    void pricesCookPricesAndAdminListsShouldUseServiceRowsOrFallback() {
        SubscriptionDurationPrice serviceDuration = durationPrice(11L, 77L, "3.0", "450.00", true);
        SubscriptionDurationPrice globalDuration = durationPrice(12L, null, "1.0", "120.00", true);
        CookSubscriptionPricing serviceCook = cookPrice(21L, 77L, FamilySize.SMALL, "250.00", true);
        CookSubscriptionPricing globalCook = cookPrice(22L, null, FamilySize.LARGE, "500.00", true);
        when(cookFamilySizeService.label(FamilySize.SMALL)).thenReturn("1-2 Members");
        when(cookFamilySizeService.label(FamilySize.LARGE)).thenReturn("5-6 Members");
        when(durationRepository.findByServiceIdAndActiveTrueOrderByDurationHoursAsc(77L)).thenReturn(List.of(serviceDuration));
        when(durationRepository.findByServiceIdIsNullAndActiveTrueOrderByDurationHoursAsc()).thenReturn(List.of(globalDuration));
        when(durationRepository.findByActiveTrueOrderByServiceIdAscDurationHoursAsc()).thenReturn(List.of(globalDuration, serviceDuration));
        when(cookRepository.findByServiceIdAndActiveTrueOrderByFamilySizeAsc(77L)).thenReturn(List.of(serviceCook));
        when(cookRepository.findByServiceIdIsNullAndActiveTrueOrderByFamilySizeAsc()).thenReturn(List.of(globalCook));
        when(cookRepository.findByActiveTrueOrderByServiceIdAscFamilySizeAsc()).thenReturn(List.of(globalCook, serviceCook));

        assertEquals(1, service.prices(77L).size());
        assertEquals(1, service.prices(null).size());
        assertEquals(1, service.cookPrices(77L).size());
        assertEquals("LARGE", service.cookPrices(null).get(0).familySize());
        assertEquals(2, service.adminList().getData().size());
        assertEquals(2, service.adminCookList().getData().size());
        assertEquals("1-2 Members", service.familySizeLabel(FamilySize.SMALL));
    }

    @Test
    void quoteShouldRejectInvalidDateRangeAndMissingServiceSpecificInputs() {
        SubscriptionQuoteRequest reversedDates = new SubscriptionQuoteRequest(
                5L, "cleaning", null, new BigDecimal("1.0"),
                List.of("Monday"), LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 1));
        SubscriptionQuoteRequest missingCookFamily = new SubscriptionQuoteRequest(
                5L, "cook", null, null,
                List.of("Monday"), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1));
        List<String> mixedDays = new ArrayList<>();
        mixedDays.add(null);
        mixedDays.add(" ");
        mixedDays.add("monday");
        mixedDays.add("MONDAY");
        SubscriptionQuoteRequest missingCleaningDuration = new SubscriptionQuoteRequest(
                5L, "", null, null,
                mixedDays, LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 2));

        assertThrows(SubscriptionValidationException.class, () -> service.quote(reversedDates));
        assertThrows(SubscriptionValidationException.class, () -> service.quote(missingCookFamily));
        assertThrows(SubscriptionValidationException.class, () -> service.quote(missingCleaningDuration));
    }

    @Test
    void resolveAndMutationMethodsShouldCoverFallbackDefaultsAndNotFoundBranches() {
        SubscriptionDurationPrice globalDuration = durationPrice(31L, null, "2.5", "333.00", true);
        CookSubscriptionPricing globalCook = cookPrice(41L, null, FamilySize.XLARGE, "777.00", true);
        when(durationRepository.findByServiceIdAndDurationHoursAndActiveTrue(88L, new BigDecimal("2.5")))
                .thenReturn(Optional.empty());
        when(durationRepository.findByServiceIdIsNullAndDurationHoursAndActiveTrue(new BigDecimal("2.5")))
                .thenReturn(Optional.of(globalDuration));
        when(durationRepository.findByServiceIdAndDurationHoursAndActiveTrue(88L, new BigDecimal("9.5")))
                .thenReturn(Optional.empty());
        when(durationRepository.findByServiceIdIsNullAndDurationHoursAndActiveTrue(new BigDecimal("9.5")))
                .thenReturn(Optional.empty());
        when(cookRepository.findByServiceIdAndFamilySizeAndActiveTrue(88L, FamilySize.XLARGE))
                .thenReturn(Optional.empty());
        when(cookRepository.findByServiceIdIsNullAndFamilySizeAndActiveTrue(FamilySize.XLARGE))
                .thenReturn(Optional.of(globalCook));
        when(cookRepository.findByServiceIdAndFamilySizeAndActiveTrue(88L, FamilySize.SMALL))
                .thenReturn(Optional.empty());
        when(cookRepository.findByServiceIdIsNullAndFamilySizeAndActiveTrue(FamilySize.SMALL))
                .thenReturn(Optional.empty());
        when(durationRepository.findById(501L)).thenReturn(Optional.empty());
        when(cookRepository.findById(601L)).thenReturn(Optional.empty());
        SubscriptionDurationPrice deleteDuration = durationPrice(701L, null, "1.0", "111.00", true);
        when(durationRepository.findById(701L)).thenReturn(Optional.of(deleteDuration));
        CookSubscriptionPricing updateCook = cookPrice(801L, null, FamilySize.MEDIUM, "444.00", true);
        when(cookRepository.findById(801L)).thenReturn(Optional.of(updateCook));
        when(cookRepository.save(any(CookSubscriptionPricing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(new BigDecimal("333.00"), service.resolvePerVisitPrice(88L, new BigDecimal("2.5")));
        assertThrows(SubscriptionValidationException.class, () -> service.resolvePerVisitPrice(88L, new BigDecimal("9.5")));
        assertEquals(new BigDecimal("777.00"), service.resolveCookPricePerVisit(88L, FamilySize.XLARGE));
        assertThrows(SubscriptionValidationException.class, () -> service.resolveCookPricePerVisit(88L, FamilySize.SMALL));
        assertThrows(ResourceNotFoundException.class, () -> service.update(501L, new SubscriptionDurationPriceRequest(
                null, new BigDecimal("1.0"), new BigDecimal("99.00"), true)));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteCook(601L));

        assertEquals("Subscription duration price deleted successfully", service.delete(701L).getMessage());
        assertTrue(service.updateCook(801L, new CookSubscriptionPricingRequest(
                null, FamilySize.MEDIUM, new BigDecimal("555.00"), null)).getData().active());
    }

    private SubscriptionDurationPrice durationPrice(Long id, Long serviceId, String hours, String price, boolean active) {
        SubscriptionDurationPrice row = new SubscriptionDurationPrice();
        ReflectionTestUtils.setField(row, "id", id);
        row.setServiceId(serviceId);
        row.setDurationHours(new BigDecimal(hours));
        row.setPerVisitPrice(new BigDecimal(price));
        row.setActive(active);
        return row;
    }

    private CookSubscriptionPricing cookPrice(Long id, Long serviceId, FamilySize familySize, String price, boolean active) {
        CookSubscriptionPricing row = new CookSubscriptionPricing();
        ReflectionTestUtils.setField(row, "id", id);
        row.setServiceId(serviceId);
        row.setFamilySize(familySize);
        row.setPricePerVisit(new BigDecimal(price));
        row.setActive(active);
        return row;
    }

    private CookCoverageResponse coverage(FamilySize familySize, String itemName) {
        return new CookCoverageResponse(
                null,
                null,
                null,
                familySize.name(),
                "3-4",
                familySize.name() + " Members",
                "Standard home cooking",
                null,
                null,
                List.of(new CookCoverageItemResponse(null, itemName, itemName.toLowerCase(), 1, 1))
        );
    }
}
