package com.lexho.user_service.addon.service;

import com.lexho.user_service.addon.dto.AddonCategoryMappingRequest;
import com.lexho.user_service.addon.dto.AddonMappingResponse;
import com.lexho.user_service.addon.dto.AddonResponse;
import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.addon.dto.AddonServiceMappingRequest;
import com.lexho.user_service.addon.dto.AddonStatusRequest;
import com.lexho.user_service.addon.dto.AddonUpsertRequest;
import com.lexho.user_service.addon.dto.CustomerAddonResponse;
import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.user_service.addon.entity.CategoryAddonEntity;
import com.lexho.user_service.addon.entity.ServiceAddonEntity;
import com.lexho.user_service.addon.repository.AddonRepository;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.repository.CategoryAddonRepository;
import com.lexho.user_service.addon.repository.ServiceAddonRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AddonManagementService {
    private final AddonRepository addonRepository;
    private final ServiceAddonRepository serviceAddonRepository;
    private final CategoryAddonRepository categoryAddonRepository;
    private final CartItemAddonRepository cartItemAddonRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final AdminCategoryRepository categoryRepository;

    public AddonManagementService(AddonRepository addonRepository,
                                  ServiceAddonRepository serviceAddonRepository,
                                  CategoryAddonRepository categoryAddonRepository,
                                  CartItemAddonRepository cartItemAddonRepository,
                                  AdminServiceEntityRepository serviceRepository,
                                  AdminCategoryRepository categoryRepository) {
        this.addonRepository = addonRepository;
        this.serviceAddonRepository = serviceAddonRepository;
        this.categoryAddonRepository = categoryAddonRepository;
        this.cartItemAddonRepository = cartItemAddonRepository;
        this.serviceRepository = serviceRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public AddonResponse create(AddonUpsertRequest request) {
        AddonEntity addon = new AddonEntity();
        apply(addon, request, true);
        AddonEntity saved = addonRepository.save(addon);
        syncInlineMappings(saved.getId(), request);
        return toResponse(saved);
    }

    @Transactional
    public AddonResponse update(Long addonId, AddonUpsertRequest request) {
        AddonEntity addon = findAddon(addonId);
        apply(addon, request, false);
        AddonEntity saved = addonRepository.save(addon);
        syncInlineMappings(saved.getId(), request);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AddonResponse details(Long addonId) {
        return toResponse(findAddon(addonId));
    }

    @Transactional(readOnly = true)
    public Page<AddonResponse> list(int page, int size, String search) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        String normalized = search == null ? null : search.trim();
        Page<AddonEntity> result = normalized == null || normalized.isBlank()
                ? addonRepository.findAll(pageable)
                : addonRepository.findByAddonNameContainingIgnoreCase(normalized, pageable);
        return result.map(this::toResponse);
    }

    @Transactional
    public AddonResponse updateStatus(Long addonId, AddonStatusRequest request) {
        AddonEntity addon = findAddon(addonId);
        addon.setActive(Boolean.TRUE.equals(request.getActive()));
        return toResponse(addonRepository.save(addon));
    }

    @Transactional
    public void delete(Long addonId) {
        AddonEntity addon = findAddon(addonId);
        serviceAddonRepository.deleteByAddonId(addon.getId());
        categoryAddonRepository.deleteByAddonId(addon.getId());
        cartItemAddonRepository.deleteByAddonId(addon.getId());
        addonRepository.delete(addon);
    }

    @Transactional
    public List<AddonMappingResponse> mapToServices(AddonServiceMappingRequest request) {
        findAddon(request.getAddonId());
        List<AddonMappingResponse> responses = new ArrayList<>();
        for (Long serviceId : distinctIds(request.getServiceIds())) {
            serviceRepository.findByIdAndDeletedFalse(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
            ServiceAddonEntity mapping = serviceAddonRepository.findByServiceIdAndAddonId(serviceId, request.getAddonId())
                    .orElseGet(ServiceAddonEntity::new);
            mapping.setServiceId(serviceId);
            mapping.setAddonId(request.getAddonId());
            ServiceAddonEntity saved = serviceAddonRepository.save(mapping);
            responses.add(new AddonMappingResponse(saved.getId(), saved.getAddonId(), saved.getServiceId(), "SERVICE", saved.getCreatedAt()));
        }
        return responses;
    }

    @Transactional
    public List<AddonMappingResponse> mapToCategories(AddonCategoryMappingRequest request) {
        findAddon(request.getAddonId());
        List<AddonMappingResponse> responses = new ArrayList<>();
        for (Long categoryId : distinctIds(request.getCategoryIds())) {
            categoryRepository.findByIdAndDeletedFalse(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
            CategoryAddonEntity mapping = categoryAddonRepository.findByCategoryIdAndAddonId(categoryId, request.getAddonId())
                    .orElseGet(CategoryAddonEntity::new);
            mapping.setCategoryId(categoryId);
            mapping.setAddonId(request.getAddonId());
            CategoryAddonEntity saved = categoryAddonRepository.save(mapping);
            responses.add(new AddonMappingResponse(-saved.getId(), saved.getAddonId(), saved.getCategoryId(), "CATEGORY", saved.getCreatedAt()));
        }
        return responses;
    }

    @Transactional
    public void removeMapping(Long mappingId) {
        if (mappingId == null) {
            throw new BadRequestException("mappingId is required");
        }
        if (mappingId != null && mappingId < 0) {
            Long categoryMappingId = Math.abs(mappingId);
            if (categoryAddonRepository.existsById(categoryMappingId)) {
                categoryAddonRepository.deleteById(categoryMappingId);
                return;
            }
            throw new ResourceNotFoundException("Mapping not found");
        }
        if (serviceAddonRepository.existsById(mappingId)) {
            serviceAddonRepository.deleteById(mappingId);
            return;
        }
        if (categoryAddonRepository.existsById(mappingId)) {
            categoryAddonRepository.deleteById(mappingId);
            return;
        }
        throw new ResourceNotFoundException("Mapping not found");
    }

    @Transactional(readOnly = true)
    public List<CustomerAddonResponse> availableAddonsForService(AdminServiceEntity service) {
        if (service == null || service.getId() == null) {
            return List.of();
        }
        return loadAvailableAddonsForService(service);
    }

    private List<CustomerAddonResponse> loadAvailableAddonsForService(AdminServiceEntity service) {
        Map<Long, Long> orderedAddonIds = new LinkedHashMap<>();
        if (service.getCategory() != null && service.getCategory().getId() != null) {
            for (CategoryAddonEntity mapping : categoryAddonRepository.findByCategoryId(service.getCategory().getId())) {
                orderedAddonIds.put(mapping.getAddonId(), mapping.getAddonId());
            }
        }
        for (ServiceAddonEntity mapping : serviceAddonRepository.findByServiceId(service.getId())) {
            orderedAddonIds.remove(mapping.getAddonId());
            orderedAddonIds.put(mapping.getAddonId(), mapping.getAddonId());
        }
        if (orderedAddonIds.isEmpty()) {
            return List.of();
        }
        Map<Long, AddonEntity> addons = byId(addonRepository.findByIdInAndActiveTrue(orderedAddonIds.keySet()));
        return orderedAddonIds.keySet().stream()
                .map(addons::get)
                .filter(addon -> addon != null && addon.isActive())
                .map(this::toCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ValidatedAddonSelection validateSelections(AdminServiceEntity service, List<AddonSelectionRequest> selected) {
        if (service == null || service.getId() == null) {
            throw new BadRequestException("serviceId is required for add-ons");
        }
        List<CustomerAddonResponse> available = loadAvailableAddonsForService(service);
        Map<Long, CustomerAddonResponse> availableById = availableAddonsById(available);
        Map<Long, Integer> quantities = requestedQuantities(selected, availableById);
        applyRequiredAddons(available, quantities);
        return new ValidatedAddonSelection(resolveValidatedAddons(quantities));
    }

    private Map<Long, CustomerAddonResponse> availableAddonsById(List<CustomerAddonResponse> available) {
        Map<Long, CustomerAddonResponse> availableById = new HashMap<>();
        for (CustomerAddonResponse addon : available) {
            availableById.put(addon.getAddonId(), addon);
        }
        return availableById;
    }

    private Map<Long, Integer> requestedQuantities(List<AddonSelectionRequest> selected,
                                                   Map<Long, CustomerAddonResponse> availableById) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        if (selected == null) {
            return quantities;
        }
        for (AddonSelectionRequest item : selected) {
            addRequestedQuantity(item, availableById, quantities);
        }
        return quantities;
    }

    private void addRequestedQuantity(AddonSelectionRequest item, Map<Long, CustomerAddonResponse> availableById,
                                      Map<Long, Integer> quantities) {
        Long addonId = requestedAddonId(item);
        CustomerAddonResponse addon = availableAddon(addonId, availableById);
        int requestedQuantity = item.getQuantity() == null ? 1 : item.getQuantity();
        validateQuantity(addon, requestedQuantity);
        quantities.merge(addonId, requestedQuantity, Integer::sum);
        validateQuantity(addon, quantities.get(addonId));
    }

    private Long requestedAddonId(AddonSelectionRequest item) {
        if (item == null || item.getAddonId() == null) {
            throw new BadRequestException("addonId is required");
        }
        return item.getAddonId();
    }

    private CustomerAddonResponse availableAddon(Long addonId, Map<Long, CustomerAddonResponse> availableById) {
        CustomerAddonResponse addon = availableById.get(addonId);
        if (addon == null) {
            throw new CodedValidationException("ADDON_UNAVAILABLE",
                    "Add-on is not active or not mapped to this service: " + addonId);
        }
        return addon;
    }

    private void applyRequiredAddons(List<CustomerAddonResponse> available, Map<Long, Integer> quantities) {
        for (CustomerAddonResponse addon : available) {
            if (addon.isRequired() && !quantities.containsKey(addon.getAddonId())) {
                int quantity = addon.getMinQuantity() == null ? 1 : addon.getMinQuantity();
                quantities.put(addon.getAddonId(), quantity);
            }
        }
    }

    private List<ValidatedAddon> resolveValidatedAddons(Map<Long, Integer> quantities) {
        List<ValidatedAddon> resolved = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            AddonEntity addon = findAddon(entry.getKey());
            if (!addon.isActive()) {
                throw new CodedValidationException("ADDON_UNAVAILABLE",
                        "Inactive add-on cannot be selected: " + addon.getId());
            }
            resolved.add(new ValidatedAddon(addon, entry.getValue()));
        }
        return resolved;
    }

    private void validateQuantity(CustomerAddonResponse addon, int quantity) {
        if (quantity < 1) {
            throw new CodedValidationException("QUANTITY_EXCEEDED", "Add-on quantity must be at least 1");
        }
        if (!addon.isQuantityAllowed() && quantity != 1) {
            throw new CodedValidationException("QUANTITY_EXCEEDED", "Quantity is not allowed for add-on: " + addon.getAddonId());
        }
        Integer min = addon.getMinQuantity() == null ? 1 : addon.getMinQuantity();
        Integer max = addon.getMaxQuantity() == null ? min : addon.getMaxQuantity();
        if (quantity < min || quantity > max) {
            throw new CodedValidationException("QUANTITY_EXCEEDED",
                    "Add-on quantity must be between " + min + " and " + max + " for addonId=" + addon.getAddonId());
        }
    }

    public AddonResponse toResponse(AddonEntity addon) {
        AddonResponse response = new AddonResponse();
        response.setAddonId(addon.getId());
        response.setAddonName(addon.getAddonName());
        response.setDescription(addon.getDescription());
        response.setPrice(addon.getPrice());
        response.setActive(addon.isActive());
        response.setQuantityAllowed(addon.isQuantityAllowed());
        response.setMinQuantity(addon.getMinQuantity());
        response.setMaxQuantity(addon.getMaxQuantity());
        response.setRequired(addon.isRequired());
        response.setCategoryIds(categoryAddonRepository.findByAddonId(addon.getId()).stream()
                .map(CategoryAddonEntity::getCategoryId)
                .toList());
        response.setServiceIds(serviceAddonRepository.findByAddonId(addon.getId()).stream()
                .map(ServiceAddonEntity::getServiceId)
                .toList());
        response.setCreatedAt(addon.getCreatedAt());
        response.setUpdatedAt(addon.getUpdatedAt());
        return response;
    }

    public CustomerAddonResponse toCustomerResponse(AddonEntity addon) {
        CustomerAddonResponse response = new CustomerAddonResponse();
        response.setAddonId(addon.getId());
        response.setAddonName(addon.getAddonName());
        response.setDescription(addon.getDescription());
        response.setPrice(addon.getPrice());
        response.setRequired(addon.isRequired());
        response.setQuantityAllowed(addon.isQuantityAllowed());
        response.setMinQuantity(addon.getMinQuantity());
        response.setMaxQuantity(addon.getMaxQuantity());
        return response;
    }

    private void apply(AddonEntity addon, AddonUpsertRequest request, boolean create) {
        addon.setAddonName(request.getAddonName().trim());
        addon.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        addon.setPrice(request.getPrice());
        addon.setActive(request.getActive() == null ? create || addon.isActive() : request.getActive());
        addon.setQuantityAllowed(Boolean.TRUE.equals(request.getQuantityAllowed()));
        addon.setMinQuantity(request.getMinQuantity() == null ? 1 : request.getMinQuantity());
        addon.setMaxQuantity(request.getMaxQuantity() == null ? addon.getMinQuantity() : request.getMaxQuantity());
        addon.setRequired(Boolean.TRUE.equals(request.getRequired()));
        addon.normalizeQuantities();
        if (addon.getPrice() == null || addon.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("price must be non-negative");
        }
        if (addon.getMaxQuantity() < addon.getMinQuantity()) {
            throw new BadRequestException("maxQuantity must be greater than or equal to minQuantity");
        }
    }

    private void syncInlineMappings(Long addonId, AddonUpsertRequest request) {
        if (request.getCategoryIds() != null) {
            syncCategoryMappings(addonId, optionalDistinctIds(request.getCategoryIds()));
        }
        if (request.getServiceIds() != null) {
            syncServiceMappings(addonId, optionalDistinctIds(request.getServiceIds()));
        }
    }

    private void syncCategoryMappings(Long addonId, Set<Long> requestedCategoryIds) {
        validateCategoryIds(requestedCategoryIds);
        deleteStaleCategoryMappings(addonId, requestedCategoryIds);
        createMissingCategoryMappings(addonId, requestedCategoryIds);
    }

    private void validateCategoryIds(Set<Long> categoryIds) {
        for (Long categoryId : categoryIds) {
            categoryRepository.findByIdAndDeletedFalse(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        }
    }

    private void deleteStaleCategoryMappings(Long addonId, Set<Long> requestedCategoryIds) {
        for (CategoryAddonEntity existing : categoryAddonRepository.findByAddonId(addonId)) {
            if (!requestedCategoryIds.contains(existing.getCategoryId())) {
                categoryAddonRepository.delete(existing);
            }
        }
    }

    private void createMissingCategoryMappings(Long addonId, Set<Long> requestedCategoryIds) {
        for (Long categoryId : requestedCategoryIds) {
            if (categoryAddonRepository.findByCategoryIdAndAddonId(categoryId, addonId).isPresent()) {
                continue;
            }
            CategoryAddonEntity mapping = new CategoryAddonEntity();
            mapping.setAddonId(addonId);
            mapping.setCategoryId(categoryId);
            categoryAddonRepository.save(mapping);
        }
    }

    private void syncServiceMappings(Long addonId, Set<Long> requestedServiceIds) {
        validateServiceIds(requestedServiceIds);
        deleteStaleServiceMappings(addonId, requestedServiceIds);
        createMissingServiceMappings(addonId, requestedServiceIds);
    }

    private void validateServiceIds(Set<Long> serviceIds) {
        for (Long serviceId : serviceIds) {
            serviceRepository.findByIdAndDeletedFalse(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        }
    }

    private void deleteStaleServiceMappings(Long addonId, Set<Long> requestedServiceIds) {
        for (ServiceAddonEntity existing : serviceAddonRepository.findByAddonId(addonId)) {
            if (!requestedServiceIds.contains(existing.getServiceId())) {
                serviceAddonRepository.delete(existing);
            }
        }
    }

    private void createMissingServiceMappings(Long addonId, Set<Long> requestedServiceIds) {
        for (Long serviceId : requestedServiceIds) {
            if (serviceAddonRepository.findByServiceIdAndAddonId(serviceId, addonId).isPresent()) {
                continue;
            }
            ServiceAddonEntity mapping = new ServiceAddonEntity();
            mapping.setAddonId(addonId);
            mapping.setServiceId(serviceId);
            serviceAddonRepository.save(mapping);
        }
    }

    private AddonEntity findAddon(Long addonId) {
        if (addonId == null) {
            throw new BadRequestException("addonId is required");
        }
        return addonRepository.findById(addonId)
                .orElseThrow(() -> new ResourceNotFoundException("Add-on not found"));
    }

    private Set<Long> distinctIds(Collection<Long> ids) {
        Set<Long> result = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("mapping target ids are required");
        }
        for (Long id : ids) {
            if (id == null) {
                throw new BadRequestException("mapping target id cannot be null");
            }
            result.add(id);
        }
        return result;
    }

    private Set<Long> optionalDistinctIds(Collection<Long> ids) {
        Set<Long> result = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        for (Long id : ids) {
            if (id == null) {
                throw new BadRequestException("mapping target id cannot be null");
            }
            result.add(id);
        }
        return result;
    }

    private Map<Long, AddonEntity> byId(List<AddonEntity> addons) {
        Map<Long, AddonEntity> result = new HashMap<>();
        for (AddonEntity addon : addons) {
            result.put(addon.getId(), addon);
        }
        return result;
    }

    public static class ValidatedAddonSelection {
        private final List<ValidatedAddon> addons;

        public ValidatedAddonSelection(List<ValidatedAddon> addons) {
            this.addons = addons;
        }

        public List<ValidatedAddon> getAddons() { return addons; }
    }

    public static class ValidatedAddon {
        private final AddonEntity addon;
        private final int quantity;

        public ValidatedAddon(AddonEntity addon, int quantity) {
            this.addon = addon;
            this.quantity = quantity;
        }

        public AddonEntity getAddon() { return addon; }
        public int getQuantity() { return quantity; }
        public BigDecimal totalPrice() { return addon.getPrice().multiply(BigDecimal.valueOf(quantity)); }
    }
}
