package com.lexho.user_service.office.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.office.dto.ScreeningCentreAdminResponse;
import com.lexho.user_service.office.dto.ScreeningCentrePartnerResponse;
import com.lexho.user_service.office.dto.ScreeningCentreRequest;
import com.lexho.user_service.office.entity.OfficeLocation;
import com.lexho.user_service.office.repository.OfficeLocationRepository;
import com.lexho.user_service.util.LexhoConstants;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
public class ScreeningCentreService {

    public static final String SCREENING_CENTRE = "SCREENING_CENTRE";
    private static final String OFFICE_LOCATION = "OFFICE_LOCATION";

    private final OfficeLocationRepository officeLocationRepository;
    private final AuditLogService auditLogService;

    public ScreeningCentreService(OfficeLocationRepository officeLocationRepository,
                                  AuditLogService auditLogService) {
        this.officeLocationRepository = officeLocationRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public ScreeningCentreAdminResponse getAdminScreeningCentre() {
        return officeLocationRepository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc(SCREENING_CENTRE)
                .map(this::toAdminResponse)
                .orElseThrow(this::notConfigured);
    }

    @Transactional(readOnly = true)
    public ScreeningCentrePartnerResponse getPartnerScreeningCentre() {
        return officeLocationRepository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc(SCREENING_CENTRE)
                .map(this::toPartnerResponse)
                .orElseThrow(this::notConfigured);
    }

    @Transactional
    public ScreeningCentreAdminResponse saveScreeningCentre(ScreeningCentreRequest request, Long adminId) {
        validateCoordinates(request.latitude(), request.longitude());
        String actor = actor(adminId);
        List<OfficeLocation> activeBefore = officeLocationRepository.findByTypeAndActiveTrue(SCREENING_CENTRE);
        officeLocationRepository.deactivateActiveByType(SCREENING_CENTRE, actor);
        OfficeLocation location = new OfficeLocation();
        location.setName(normalizeRequired(request.name(), "name is required"));
        location.setAddress(normalizeRequired(request.address(), "address is required"));
        location.setLatitude(scaleCoordinate(request.latitude()));
        location.setLongitude(scaleCoordinate(request.longitude()));
        location.setPhone(normalize(request.phone()));
        location.setType(SCREENING_CENTRE);
        location.setActive(true);
        location.setMapProvider(normalize(request.mapProvider()));
        location.setPlaceId(normalize(request.placeId()));
        location.setLandmark(normalize(request.landmark()));
        location.setEntranceInfo(normalize(request.entranceInfo()));
        location.setCreatedBy(actor);
        location.setUpdatedBy(actor);
        OfficeLocation saved = officeLocationRepository.save(location);
        auditLogService.log(adminId, LexhoConstants.ACTOR_ADMIN,
                activeBefore.isEmpty() ? "SCREENING_CENTRE_CREATED" : "SCREENING_CENTRE_UPDATED",
                OFFICE_LOCATION, String.valueOf(saved.getId()),
                activeBefore.isEmpty() ? null : String.valueOf(activeBefore.get(0).getId()),
                saved.getLatitude() + "," + saved.getLongitude());
        return toAdminResponse(saved);
    }

    @Transactional
    public ScreeningCentreAdminResponse updateStatus(boolean active, Long adminId) {
        String actor = actor(adminId);
        if (!active) {
            OfficeLocation current = officeLocationRepository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc(SCREENING_CENTRE)
                    .orElseThrow(this::notConfigured);
            current.setActive(false);
            current.setUpdatedBy(actor);
            OfficeLocation saved = officeLocationRepository.save(current);
        auditLogService.log(adminId, LexhoConstants.ACTOR_ADMIN, "SCREENING_CENTRE_DEACTIVATED",
                    OFFICE_LOCATION, String.valueOf(saved.getId()), "active=true", "active=false");
            return toAdminResponse(saved);
        }

        OfficeLocation latest = officeLocationRepository.findFirstByTypeOrderByUpdatedAtDesc(SCREENING_CENTRE)
                .orElseThrow(this::notConfigured);
        officeLocationRepository.deactivateActiveByType(SCREENING_CENTRE, actor);
        latest.setActive(true);
        latest.setUpdatedBy(actor);
        OfficeLocation saved = officeLocationRepository.save(latest);
        auditLogService.log(adminId, LexhoConstants.ACTOR_ADMIN, "SCREENING_CENTRE_ACTIVATED",
                OFFICE_LOCATION, String.valueOf(saved.getId()), "active=false", "active=true");
        return toAdminResponse(saved);
    }

    public void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null) {
            throw new BadRequestException("latitude is required");
        }
        if (longitude == null) {
            throw new BadRequestException("longitude is required");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Invalid latitude");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Invalid longitude");
        }
    }

    private ScreeningCentreAdminResponse toAdminResponse(OfficeLocation location) {
        return new ScreeningCentreAdminResponse(
                location.getId(),
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getPhone(),
                location.getType(),
                location.isActive(),
                location.getMapProvider(),
                location.getPlaceId(),
                location.getLandmark(),
                location.getEntranceInfo(),
                toOffset(location.getCreatedAt()),
                toOffset(location.getUpdatedAt()));
    }

    private ScreeningCentrePartnerResponse toPartnerResponse(OfficeLocation location) {
        return new ScreeningCentrePartnerResponse(
                location.getName(),
                location.getAddress(),
                location.getLatitude(),
                location.getLongitude(),
                location.getPhone());
    }

    private CodedValidationException notConfigured() {
        return new CodedValidationException("SCREENING_CENTRE_NOT_CONFIGURED",
                "Screening centre is not configured", HttpStatus.NOT_FOUND);
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private BigDecimal scaleCoordinate(BigDecimal value) {
        return value.setScale(7, RoundingMode.HALF_UP);
    }

    private String normalizeRequired(String value, String message) {
        return StringSanitizers.requireTrimmed(value, message);
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }

    private String actor(Long adminId) {
        return adminId == null
                ? LexhoConstants.ACTOR_ADMIN
                : (LexhoConstants.ACTOR_ADMIN_PREFIX + adminId).toUpperCase(Locale.ROOT);
    }
}
