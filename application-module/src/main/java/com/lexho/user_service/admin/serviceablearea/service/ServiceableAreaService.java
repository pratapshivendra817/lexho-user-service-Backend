package com.lexho.user_service.admin.serviceablearea.service;

import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaPageResponse;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaResponse;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaStatusRequest;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaUpsertRequest;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;
import java.util.Locale;

@Service
public class ServiceableAreaService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "city", "state", "pincode", "cityId", "zoneId", "active", "deleted", "createdAt", "updatedAt"
    );

    private final ServiceableAreaRepository serviceableAreaRepository;

    public ServiceableAreaService(ServiceableAreaRepository serviceableAreaRepository) {
        this.serviceableAreaRepository = serviceableAreaRepository;
    }

    @Transactional(readOnly = true)
    public ServiceableAreaPageResponse list(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = pageable(page, size, sortBy, sortDir);
        Page<ServiceableAreaEntity> result = serviceableAreaRepository.findAll(notDeletedSpec(), pageable);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public ServiceableAreaPageResponse search(String query,
                                              int page,
                                              int size,
                                              String sortBy,
                                              String sortDir) {
        Pageable pageable = pageable(page, size, sortBy, sortDir);
        Specification<ServiceableAreaEntity> spec = notDeletedSpec().and(searchSpec(query));
        Page<ServiceableAreaEntity> result = serviceableAreaRepository.findAll(spec, pageable);
        return toPageResponse(result);
    }

    @Transactional
    public ServiceableAreaResponse create(ServiceableAreaUpsertRequest request) {
        ServiceableAreaEntity entity = new ServiceableAreaEntity();
        applyUpsert(entity, request);
        entity.setActive(true);
        return toResponse(serviceableAreaRepository.save(entity));
    }

    @Transactional
    public ServiceableAreaResponse update(Long id, ServiceableAreaUpsertRequest request) {
        ServiceableAreaEntity entity = findActiveRow(id);
        applyUpsert(entity, request);
        return toResponse(serviceableAreaRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ServiceableAreaEntity entity = findActiveRow(id);
        entity.setDeleted(true);
        serviceableAreaRepository.save(entity);
    }

    @Transactional
    public ServiceableAreaResponse updateStatus(Long id, ServiceableAreaStatusRequest request) {
        ServiceableAreaEntity entity = findActiveRow(id);
        entity.setActive(Boolean.TRUE.equals(request.getActive()));
        return toResponse(serviceableAreaRepository.save(entity));
    }

    private ServiceableAreaEntity findActiveRow(Long id) {
        return serviceableAreaRepository.findById(id)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Serviceable area not found"));
    }

    private void applyUpsert(ServiceableAreaEntity entity, ServiceableAreaUpsertRequest request) {
        entity.setName(clean(request.getName()));
        entity.setCity(clean(request.getCity()));
        entity.setState(clean(request.getState()));
        entity.setPincode(clean(request.getPincode()));
        entity.setCityId(request.getCityId());
        entity.setZoneId(request.getZoneId());
        entity.setPolygonJson(clean(request.getPolygonJson()));
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private Pageable pageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = sanitizeSortBy(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy));
    }

    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        String candidate = sortBy.trim();
        return ALLOWED_SORT_FIELDS.contains(candidate) ? candidate : "id";
    }

    private Specification<ServiceableAreaEntity> notDeletedSpec() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    private Specification<ServiceableAreaEntity> searchSpec(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String q = "%" + queryText.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), q),
                cb.like(cb.lower(root.get("city")), q),
                cb.like(cb.lower(root.get("state")), q),
                cb.like(cb.lower(root.get("pincode")), q)
        );
    }

    private ServiceableAreaPageResponse toPageResponse(Page<ServiceableAreaEntity> page) {
        ServiceableAreaPageResponse response = new ServiceableAreaPageResponse();
        List<ServiceableAreaResponse> content = page.getContent().stream().map(this::toResponse).toList();
        response.setContent(content);
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }

    private ServiceableAreaResponse toResponse(ServiceableAreaEntity entity) {
        ServiceableAreaResponse response = new ServiceableAreaResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setPincode(entity.getPincode());
        response.setCityId(entity.getCityId());
        response.setZoneId(entity.getZoneId());
        response.setPolygonJson(entity.getPolygonJson());
        response.setActive(entity.isActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
