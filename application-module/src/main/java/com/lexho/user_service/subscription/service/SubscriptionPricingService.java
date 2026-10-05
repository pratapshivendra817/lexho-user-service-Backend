package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.cookfamilysize.service.CookFamilySizeService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingRequest;
import com.lexho.user_service.subscription.dto.CookSubscriptionPricingResponse;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceRequest;
import com.lexho.user_service.subscription.dto.SubscriptionDurationPriceResponse;
import com.lexho.user_service.subscription.dto.SubscriptionPriceQuoteResponse;
import com.lexho.user_service.subscription.dto.SubscriptionQuoteRequest;
import com.lexho.user_service.subscription.dto.SubscriptionQuoteResponse;
import com.lexho.user_service.subscription.entity.CookSubscriptionPricing;
import com.lexho.user_service.subscription.entity.SubscriptionDurationPrice;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.repository.CookSubscriptionPricingRepository;
import com.lexho.user_service.subscription.repository.SubscriptionDurationPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class SubscriptionPricingService {

    private final SubscriptionDurationPriceRepository durationPriceRepository;
    private final CookSubscriptionPricingRepository cookPricingRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final CookFamilySizeService cookFamilySizeService;
    private final CookCoverageService cookCoverageService;

    public SubscriptionPricingService(SubscriptionDurationPriceRepository durationPriceRepository,
                                      CookSubscriptionPricingRepository cookPricingRepository,
                                      AdminServiceEntityRepository serviceRepository,
                                      CookFamilySizeService cookFamilySizeService,
                                      CookCoverageService cookCoverageService) {
        this.durationPriceRepository = durationPriceRepository;
        this.cookPricingRepository = cookPricingRepository;
        this.serviceRepository = serviceRepository;
        this.cookFamilySizeService = cookFamilySizeService;
        this.cookCoverageService = cookCoverageService;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDurationPriceResponse> prices(Long serviceId) {
        List<SubscriptionDurationPrice> rows = serviceId == null
                ? durationPriceRepository.findByServiceIdIsNullAndActiveTrueOrderByDurationHoursAsc()
                : servicePricesWithFallback(serviceId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPriceQuoteResponse quote(Long serviceId, BigDecimal durationHours, Integer visits) {
        if (visits == null || visits <= 0) {
            throw new SubscriptionValidationException("visits must be positive");
        }
        BigDecimal perVisitPrice = resolvePerVisitPriceInternal(serviceId, durationHours);
        return new SubscriptionPriceQuoteResponse(
                serviceId,
                strip(durationHours),
                perVisitPrice,
                visits,
                perVisitPrice.multiply(BigDecimal.valueOf(visits))
        );
    }

    @Transactional(readOnly = true)
    public SubscriptionQuoteResponse quote(SubscriptionQuoteRequest request) {
        List<String> days = SubscriptionScheduleSupport.normalizeDays(request.frequencyDays());
        validateDateRange(request.startDate(), request.endDate());
        int visits = SubscriptionScheduleSupport.generateVisitDates(request.startDate(), request.endDate(), days).size();
        String serviceType = normalizeServiceType(request.serviceType());
        if ("COOK".equals(serviceType)) {
            if (request.familySize() == null) {
                throw new SubscriptionValidationException("familySize is required for COOK subscription");
            }
            BigDecimal pricePerVisit = resolveCookPricePerVisitInternal(request.serviceId(), request.familySize());
            return new SubscriptionQuoteResponse(request.serviceId(), serviceType, request.familySize().name(),
                    familySizeLabel(request.familySize()), null, pricePerVisit, visits,
                    pricePerVisit.multiply(BigDecimal.valueOf(visits)), "INR");
        }
        if (request.durationHours() == null) {
            throw new SubscriptionValidationException("durationHours is required for CLEANING subscription");
        }
        BigDecimal pricePerVisit = resolvePerVisitPriceInternal(request.serviceId(), request.durationHours());
        return new SubscriptionQuoteResponse(request.serviceId(), serviceType, null, null, strip(request.durationHours()),
                pricePerVisit, visits, pricePerVisit.multiply(BigDecimal.valueOf(visits)), "INR");
    }

    @Transactional(readOnly = true)
    public BigDecimal resolvePerVisitPrice(Long serviceId, BigDecimal durationHours) {
        return resolvePerVisitPriceInternal(serviceId, durationHours);
    }

    private BigDecimal resolvePerVisitPriceInternal(Long serviceId, BigDecimal durationHours) {
        return durationPriceRepository.findByServiceIdAndDurationHoursAndActiveTrue(serviceId, durationHours)
                .or(() -> durationPriceRepository.findByServiceIdIsNullAndDurationHoursAndActiveTrue(durationHours))
                .map(SubscriptionDurationPrice::getPerVisitPrice)
                .orElseThrow(() -> new SubscriptionValidationException("Subscription price not configured for selected duration"));
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveCookPricePerVisit(Long serviceId, FamilySize familySize) {
        return resolveCookPricePerVisitInternal(serviceId, familySize);
    }

    private BigDecimal resolveCookPricePerVisitInternal(Long serviceId, FamilySize familySize) {
        return cookPricingRepository.findByServiceIdAndFamilySizeAndActiveTrue(serviceId, familySize)
                .or(() -> cookPricingRepository.findByServiceIdIsNullAndFamilySizeAndActiveTrue(familySize))
                .map(CookSubscriptionPricing::getPricePerVisit)
                .orElseThrow(() -> new SubscriptionValidationException("Cook subscription price not configured for selected family size"));
    }

    @Transactional(readOnly = true)
    public DataResponse<List<SubscriptionDurationPriceResponse>> adminList() {
        return new DataResponse<>(durationPriceRepository.findByActiveTrueOrderByServiceIdAscDurationHoursAsc()
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public DataResponse<List<CookSubscriptionPricingResponse>> adminCookList() {
        return new DataResponse<>(cookPricingRepository.findByActiveTrueOrderByServiceIdAscFamilySizeAsc()
                .stream()
                .map(this::toCookPricingResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public List<CookSubscriptionPricingResponse> cookPrices(Long serviceId) {
        List<CookSubscriptionPricing> rows = serviceId == null
                ? cookPricingRepository.findByServiceIdIsNullAndActiveTrueOrderByFamilySizeAsc()
                : cookPricesWithFallback(serviceId);
        return rows.stream().map(this::toCookPricingResponse).toList();
    }

    @Transactional
    public DataResponse<SubscriptionDurationPriceResponse> create(SubscriptionDurationPriceRequest request) {
        validateService(request.serviceId());
        SubscriptionDurationPrice entity = new SubscriptionDurationPrice();
        applyRequest(entity, request);
        return new DataResponse<>(toResponse(durationPriceRepository.save(entity)));
    }

    @Transactional
    public DataResponse<CookSubscriptionPricingResponse> createCook(CookSubscriptionPricingRequest request) {
        validateService(request.serviceId());
        CookSubscriptionPricing entity = new CookSubscriptionPricing();
        applyCookRequest(entity, request);
        return new DataResponse<>(toCookPricingResponse(cookPricingRepository.save(entity)));
    }

    @Transactional
    public DataResponse<SubscriptionDurationPriceResponse> update(Long id, SubscriptionDurationPriceRequest request) {
        validateService(request.serviceId());
        SubscriptionDurationPrice entity = durationPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription duration price not found"));
        applyRequest(entity, request);
        return new DataResponse<>(toResponse(durationPriceRepository.save(entity)));
    }

    @Transactional
    public DataResponse<CookSubscriptionPricingResponse> updateCook(Long id, CookSubscriptionPricingRequest request) {
        validateService(request.serviceId());
        CookSubscriptionPricing entity = cookPricingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook subscription price not found"));
        applyCookRequest(entity, request);
        return new DataResponse<>(toCookPricingResponse(cookPricingRepository.save(entity)));
    }

    @Transactional
    public AdminMessageResponse delete(Long id) {
        SubscriptionDurationPrice entity = durationPriceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription duration price not found"));
        entity.setActive(false);
        durationPriceRepository.save(entity);
        return new AdminMessageResponse("Subscription duration price deleted successfully");
    }

    @Transactional
    public AdminMessageResponse deleteCook(Long id) {
        CookSubscriptionPricing entity = cookPricingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook subscription price not found"));
        entity.setActive(false);
        cookPricingRepository.save(entity);
        return new AdminMessageResponse("Cook subscription price deleted successfully");
    }

    public CookCoverageResponse cookCoverage(Long serviceId, FamilySize familySize) {
        FamilySize effectiveSize = familySize == null ? FamilySize.MEDIUM : familySize;
        return cookCoverageService.customerList(serviceId, effectiveSize).stream()
                .findFirst()
                .orElseGet(() -> new CookCoverageResponse(
                        null,
                        serviceId,
                        null,
                        effectiveSize.name(),
                        familySizeLabel(effectiveSize).replace(" Members", ""),
                        familySizeLabel(effectiveSize),
                        "Standard home cooking",
                        null,
                        null,
                        serviceId == null ? List.of() : List.of()
                ));
    }

    private List<SubscriptionDurationPrice> servicePricesWithFallback(Long serviceId) {
        List<SubscriptionDurationPrice> serviceRows = durationPriceRepository
                .findByServiceIdAndActiveTrueOrderByDurationHoursAsc(serviceId);
        return serviceRows.isEmpty()
                ? durationPriceRepository.findByServiceIdIsNullAndActiveTrueOrderByDurationHoursAsc()
                : serviceRows;
    }

    private List<CookSubscriptionPricing> cookPricesWithFallback(Long serviceId) {
        List<CookSubscriptionPricing> serviceRows = cookPricingRepository
                .findByServiceIdAndActiveTrueOrderByFamilySizeAsc(serviceId);
        return serviceRows.isEmpty()
                ? cookPricingRepository.findByServiceIdIsNullAndActiveTrueOrderByFamilySizeAsc()
                : serviceRows;
    }

    private void validateService(Long serviceId) {
        if (serviceId != null && serviceRepository.findByIdAndDeletedFalse(serviceId).isEmpty()) {
            throw new ResourceNotFoundException("Service not found");
        }
    }

    private void applyRequest(SubscriptionDurationPrice entity, SubscriptionDurationPriceRequest request) {
        entity.setServiceId(request.serviceId());
        entity.setDurationHours(request.durationHours());
        entity.setPerVisitPrice(request.perVisitPrice());
        entity.setActive(request.active() == null || request.active());
    }

    private void applyCookRequest(CookSubscriptionPricing entity, CookSubscriptionPricingRequest request) {
        entity.setServiceId(request.serviceId());
        entity.setFamilySize(request.familySize());
        entity.setPricePerVisit(request.pricePerVisit());
        entity.setActive(request.active() == null || request.active());
    }

    private SubscriptionDurationPriceResponse toResponse(SubscriptionDurationPrice entity) {
        return new SubscriptionDurationPriceResponse(
                entity.getId(),
                entity.getServiceId(),
                strip(entity.getDurationHours()),
                entity.getPerVisitPrice(),
                entity.isActive()
        );
    }

    private CookSubscriptionPricingResponse toCookPricingResponse(CookSubscriptionPricing entity) {
        return new CookSubscriptionPricingResponse(
                entity.getId(),
                entity.getServiceId(),
                entity.getFamilySize().name(),
                familySizeLabel(entity.getFamilySize()),
                entity.getPricePerVisit(),
                entity.isActive()
        );
    }

    public String familySizeLabel(FamilySize familySize) {
        return cookFamilySizeService.label(familySize);
    }

    private String normalizeServiceType(String serviceType) {
        return serviceType == null || serviceType.isBlank() ? "CLEANING" : serviceType.trim().toUpperCase(Locale.ROOT);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new SubscriptionValidationException("startDate must not be after endDate");
        }
    }

    private BigDecimal strip(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
