package com.lexho.user_service.customer.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.addon.dto.CartAddonResponse;
import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import com.lexho.user_service.addon.repository.AddonRepository;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import com.lexho.user_service.cartconfig.service.CartConfigService;
import com.lexho.user_service.cookfamilysize.service.CookFamilySizeService;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.service.SubscriptionPricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class CustomerCartServiceTest {

    @Mock
    private CustomerCartItemRepository customerCartItemRepository;
    @Mock
    private CustomerCatalogRepository customerCatalogRepository;
    @Mock
    private LexhoAddressRepository lexhoAddressRepository;
    @Mock
    private LexhoUserProfileRepository lexhoUserProfileRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AddonManagementService addonManagementService;
    @Mock
    private CartItemAddonRepository cartItemAddonRepository;
    @Mock
    private AddonRepository addonRepository;
    @Mock
    private CartConfigService cartConfigService;
    @Mock
    private SubscriptionPricingService subscriptionPricingService;
    @Mock
    private CookFamilySizeService cookFamilySizeService;

    @InjectMocks
    private CustomerCartService customerCartService;

    @Test
    void shouldCreateCartItemWhenNoExistingItem() {
        AdminServiceEntity service = new AdminServiceEntity();
        setId(service, 5L);
        service.setActive(true);
        service.setDeleted(false);

        CustomerAddToCartRequest request = new CustomerAddToCartRequest();
        request.setServiceId(5L);
        request.setQuantity(2);

        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(addonManagementService.validateSelections(any(AdminServiceEntity.class), any()))
                .thenReturn(new AddonManagementService.ValidatedAddonSelection(List.of()));
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(11L, 5L,
                "FAMILY:NONE::NONE")).thenReturn(Optional.empty());
        when(customerCartItemRepository.countByUserId(11L)).thenReturn(0L);
        when(cartItemAddonRepository.findByCartItemId(100L)).thenReturn(List.of());
        when(customerCartItemRepository.save(any(CustomerCartItemEntity.class))).thenAnswer(inv -> {
            CustomerCartItemEntity entity = inv.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        CustomerCartItemResponse response = customerCartService.addToCart(11L, request);

        assertEquals(100L, response.getItemId());
        assertEquals(5L, response.getServiceId());
        assertEquals(2, response.getQuantity());
    }

    @Test
    void shouldIncreaseQuantityWhenItemExists() {
        AdminServiceEntity service = new AdminServiceEntity();
        setId(service, 5L);
        service.setActive(true);
        service.setDeleted(false);

        CustomerCartItemEntity existing = new CustomerCartItemEntity();
        existing.setId(10L);
        existing.setUserId(11L);
        existing.setServiceId(5L);
        existing.setQuantity(1);

        CustomerAddToCartRequest request = new CustomerAddToCartRequest();
        request.setServiceId(5L);
        request.setQuantity(3);

        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(addonManagementService.validateSelections(any(AdminServiceEntity.class), any()))
                .thenReturn(new AddonManagementService.ValidatedAddonSelection(List.of()));
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(11L, 5L,
                "FAMILY:NONE::NONE")).thenReturn(Optional.of(existing));
        when(cartItemAddonRepository.findByCartItemId(10L)).thenReturn(List.of());
        when(customerCartItemRepository.save(any(CustomerCartItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        customerCartService.addToCart(11L, request);

        ArgumentCaptor<CustomerCartItemEntity> captor = ArgumentCaptor.forClass(CustomerCartItemEntity.class);
        verify(customerCartItemRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getQuantity());
    }

    @Test
    void shouldThrowWhenServiceNotFound() {
        CustomerAddToCartRequest request = new CustomerAddToCartRequest();
        request.setServiceId(99L);
        request.setQuantity(1);
        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        when(customerCatalogRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(11L, request));
    }

    @Test
    void addToCartShouldRejectInvalidUserAndQuantityBounds() {
        CustomerAddToCartRequest request = new CustomerAddToCartRequest();
        request.setServiceId(5L);
        request.setQuantity(0);
        assertThrows(BadRequestException.class, () -> customerCartService.addToCart(null, request));
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(1L, request));

        CartConfigEntity config = new CartConfigEntity();
        config.setMinServiceQuantity(2);
        config.setMaxServiceQuantity(3);
        when(cartConfigService.currentEntity()).thenReturn(config);
        request.setQuantity(1);
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(1L, request));
        request.setQuantity(4);
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(1L, request));
        verify(customerCatalogRepository, never()).findById(any());
    }

    @Test
    void addToCartShouldRejectCartLimitAndMergedQuantityLimit() {
        AdminServiceEntity service = activeService(5L);
        CustomerAddToCartRequest request = request(5L, 2);
        CartConfigEntity config = new CartConfigEntity();
        config.setMaxCartItems(1);
        config.setMaxServiceQuantity(3);
        when(cartConfigService.currentEntity()).thenReturn(config);
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(addonManagementService.validateSelections(any(), any()))
                .thenReturn(new AddonManagementService.ValidatedAddonSelection(List.of()));
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(1L, 5L, "FAMILY:NONE::NONE"))
                .thenReturn(Optional.empty());
        when(customerCartItemRepository.countByUserId(1L)).thenReturn(1L);
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(1L, request));

        CustomerCartItemEntity existing = cartItem(10L, 1L, 5L, 2);
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(1L, 5L, "FAMILY:NONE::NONE"))
                .thenReturn(Optional.of(existing));
        assertThrows(CodedValidationException.class, () -> customerCartService.addToCart(1L, request));
    }

    @Test
    void getCartShouldReturnSafeEmptyResponse() {
        when(customerCartItemRepository.findByUserId(1L)).thenReturn(List.of());
        when(lexhoUserProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(lexhoAddressRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        CustomerCartResponse response = customerCartService.getCart(1L);

        assertEquals(0L, response.getCartId());
        assertEquals(0d, response.getGrandTotal());
        assertEquals("IDLE", response.getRequestStatus());
        assertEquals("MANUAL", response.getAssignmentMode());
        assertTrue(response.getItems().isEmpty());
        assertEquals("", response.getContactName());
    }

    @Test
    void getCartShouldCalculateDiscountedTotalsAndDefaultsForMissingService() {
        CustomerCartItemEntity priced = cartItem(10L, 1L, 5L, 2);
        CustomerCartItemEntity missing = cartItem(11L, 1L, 99L, null);
        AdminServiceEntity service = activeService(5L);
        service.setName("Cleaning");
        service.setCategoryName("Home Care");
        service.setBasePrice(java.math.BigDecimal.valueOf(100));
        service.setDiscountedPrice(java.math.BigDecimal.valueOf(80));
        service.setImageUrl("images/service.png");
        when(customerCartItemRepository.findByUserId(1L)).thenReturn(List.of(priced, missing));
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(customerCatalogRepository.findById(99L)).thenReturn(Optional.empty());
        when(cartItemAddonRepository.findByCartItemId(any())).thenReturn(List.of());
        when(lexhoUserProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(lexhoAddressRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        CustomerCartResponse response = customerCartService.getCart(1L);

        assertEquals(160d, response.getItemTotal());
        assertEquals(168d, response.getGrandTotal());
        assertEquals(40d, response.getTotalSaved());
        assertEquals("HOME_CARE", response.getItems().get(0).getServiceType());
        assertEquals("", response.getItems().get(1).getName());
    }

    @Test
    void updateQuantityShouldRejectMissingAndOutOfRangeItems() {
        CustomerUpdateCartItemRequest request = new CustomerUpdateCartItemRequest();
        request.setItemId(10L);
        request.setQuantity(3);
        when(customerCartItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerCartService.updateQuantity(1L, request));

        when(customerCartItemRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(cartItem(10L, 1L, 5L, 1)));
        CartConfigEntity config = new CartConfigEntity();
        config.setMinServiceQuantity(2);
        config.setMaxServiceQuantity(4);
        when(cartConfigService.currentEntity()).thenReturn(config);
        request.setQuantity(1);
        assertThrows(CodedValidationException.class, () -> customerCartService.updateQuantity(1L, request));
        request.setQuantity(5);
        assertThrows(CodedValidationException.class, () -> customerCartService.updateQuantity(1L, request));
    }

    @Test
    void updateQuantityShouldSaveAndReturnRefreshedCart() {
        CustomerCartItemEntity item = cartItem(10L, 1L, 5L, 1);
        CustomerUpdateCartItemRequest request = new CustomerUpdateCartItemRequest();
        request.setItemId(10L);
        request.setQuantity(3);
        when(customerCartItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartConfigService.currentEntity()).thenReturn(new CartConfigEntity());
        when(customerCartItemRepository.findByUserId(1L)).thenReturn(List.of(item));
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(activeService(5L)));
        when(cartItemAddonRepository.findByCartItemId(10L)).thenReturn(List.of());
        when(lexhoUserProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(lexhoAddressRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        CustomerCartResponse response = customerCartService.updateQuantity(1L, request);

        assertEquals(3, response.getItems().get(0).getQuantity());
        verify(customerCartItemRepository).save(item);
    }

    @Test
    void removeItemShouldDeleteAddonsAndOwnedItem() {
        CustomerCartItemEntity item = cartItem(10L, 1L, 5L, 1);
        when(customerCartItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(item));

        customerCartService.removeItem(1L, 10L);

        verify(cartItemAddonRepository).deleteByCartItemId(10L);
        verify(customerCartItemRepository).delete(item);
        when(customerCartItemRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerCartService.removeItem(1L, 99L));
    }

    @Test
    void configFamilySizesAndClearCartShouldDelegate() {
        customerCartService.config();
        customerCartService.cookFamilySizes();
        customerCartService.clearCart(1L);

        verify(cartConfigService).current();
        verify(cookFamilySizeService).customerList();
        verify(cartItemAddonRepository).deleteByUserId(1L);
        verify(customerCartItemRepository).deleteByUserId(1L);
    }

    @Test
    void addToCartShouldApplyFamilySizeAndAddonPricing() {
        AdminServiceEntity service = activeService(5L);
        service.setName("Cook");
        service.setCategoryName("Daily Cook");
        service.setImageUrl("http://localhost:8080/uploads/cook.png");

        AddonEntity addon = addon(9L, "Chapati", "15.50", true, true);
        CustomerAddToCartRequest request = request(5L, 2);
        request.setFamilySize("medium");

        CookFamilySizeEntity familySize = new CookFamilySizeEntity();
        familySize.setId(3L);
        familySize.setFamilySize("MEDIUM");
        familySize.setFamilySizeLabel("Medium Family");

        CartConfigEntity config = new CartConfigEntity();
        config.setMinServiceQuantity(1);
        config.setMaxServiceQuantity(10);
        config.setMaxCartItems(5);

        when(cartConfigService.currentEntity()).thenReturn(config);
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(cookFamilySizeService.requireActive(null, "medium")).thenReturn(familySize);
        when(addonManagementService.validateSelections(any(AdminServiceEntity.class), any()))
                .thenReturn(new AddonManagementService.ValidatedAddonSelection(List.of(
                        new AddonManagementService.ValidatedAddon(addon, 2)
                )));
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(11L, 5L, "FAMILY:MEDIUM::9:2"))
                .thenReturn(Optional.empty());
        when(customerCartItemRepository.countByUserId(11L)).thenReturn(0L);
        when(customerCartItemRepository.save(any(CustomerCartItemEntity.class))).thenAnswer(invocation -> {
            CustomerCartItemEntity entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });
        CartItemAddonEntity row = new CartItemAddonEntity();
        row.setAddonId(9L);
        row.setQuantity(2);
        when(cartItemAddonRepository.findByCartItemId(100L)).thenReturn(List.of(row));
        when(addonRepository.findByIdIn(any())).thenReturn(List.of(addon));
        when(subscriptionPricingService.resolveCookPricePerVisit(5L, FamilySize.MEDIUM)).thenReturn(new BigDecimal("199.99"));

        CustomerCartItemResponse response = customerCartService.addToCart(11L, request);

        assertEquals(3L, response.getFamilySizeId());
        assertEquals("MEDIUM", response.getFamilySize());
        assertEquals("Medium Family", response.getFamilySizeLabel());
        assertEquals(199.99d, response.getPrice());
        assertEquals("https://cdn.lexho.com/uploads/cook.png", response.getImageUrl());
        assertEquals(430.98d, response.getTotalPrice());
        assertEquals(1, response.getAddons().size());
        assertEquals(31.0d, response.getAddons().get(0).getTotalPrice());
    }

    @Test
    void getCartShouldMapAddressContactStatusAndMatchedPartners() {
        CustomerCartItemEntity item = cartItem(10L, 1L, 5L, 2);
        item.setFamilySize("SMALL");
        item.setFamilySizeId(4L);
        item.setFamilySizeLabel("Small Family");

        AdminServiceEntity service = activeService(5L);
        service.setName("Cleaning");
        service.setCategoryName("Deep Clean");
        service.setImageUrl("images/deep-clean.png");
        setCategoryId(service, 22L);

        CartItemAddonEntity addonRow = new CartItemAddonEntity();
        addonRow.setAddonId(8L);
        addonRow.setQuantity(1);
        AddonEntity addon = addon(8L, "Sofa", "25.00", false, false);

        LexhoUserProfile profile = new LexhoUserProfile();
        profile.setName("Aman");
        profile.setMobile("9999999999");

        LexhoAddress address = new LexhoAddress();
        setId(address, 100L);
        address.setHouseNo("H-12");
        address.setArea("Sector 62");
        address.setCity("Noida");
        address.setPincode("201301");
        address.setDefault(true);

        Order order = new Order();
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        order.setScheduledAt(LocalDateTime.of(2026, 6, 21, 9, 15));

        Partner available = new Partner();
        available.setAvailable(true);
        available.setCategoryId(22L);
        Partner unavailable = new Partner();
        unavailable.setAvailable(false);
        unavailable.setCategoryId(22L);

        when(customerCartItemRepository.findByUserId(1L)).thenReturn(List.of(item));
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(cartItemAddonRepository.findByCartItemId(10L)).thenReturn(List.of(addonRow));
        when(addonRepository.findByIdIn(any())).thenReturn(List.of(addon));
        when(subscriptionPricingService.resolveCookPricePerVisit(5L, FamilySize.SMALL)).thenReturn(new BigDecimal("110.00"));
        when(lexhoUserProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(lexhoAddressRepository.findByUserId(1L)).thenReturn(List.of(address));
        when(orderRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(order));
        when(partnerRepository.findAll()).thenReturn(List.of(available, unavailable));

        CustomerCartResponse response = customerCartService.getCart(1L);

        assertEquals("ACCEPTED", response.getRequestStatus());
        assertEquals("AUTO", response.getAssignmentMode());
        assertEquals(1, response.getMatchedPartners());
        assertEquals("Aman", response.getContactName());
        assertEquals("9999999999", response.getContactPhone());
        assertEquals("H-12, Sector 62, Noida", response.getAddress().getFullAddress());
        assertEquals("https://cdn.lexho.com/images/deep-clean.png", response.getItems().get(0).getImageUrl());
        assertTrue(response.getScheduledTime().contains("2026-06-21T03:45:00Z"));
        assertEquals(257.25d, response.getGrandTotal());
    }

    @Test
    void updateQuantityShouldMergeDuplicateSelectionAndDeleteOriginalRow() {
        CustomerCartItemEntity item = cartItem(10L, 1L, 5L, 2);
        item.setFamilySize("SMALL");
        item.setAddonSignature("FAMILY:SMALL::NONE");

        CustomerCartItemEntity duplicate = cartItem(11L, 1L, 5L, 1);
        duplicate.setAddonSignature("FAMILY:LARGE::NONE");

        CookFamilySizeEntity familySize = new CookFamilySizeEntity();
        familySize.setId(5L);
        familySize.setFamilySize("LARGE");
        familySize.setFamilySizeLabel("Large Family");

        CustomerUpdateCartItemRequest request = new CustomerUpdateCartItemRequest();
        request.setItemId(10L);
        request.setFamilySizeId(5L);

        CartConfigEntity config = new CartConfigEntity();
        config.setMinServiceQuantity(1);
        config.setMaxServiceQuantity(5);

        when(customerCartItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartConfigService.currentEntity()).thenReturn(config);
        when(cookFamilySizeService.requireActive(5L, null)).thenReturn(familySize);
        when(cartItemAddonRepository.findByCartItemId(10L)).thenReturn(List.of());
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(1L, 5L, "FAMILY:LARGE::NONE"))
                .thenReturn(Optional.of(duplicate));
        when(customerCartItemRepository.findByUserId(1L)).thenReturn(List.of(duplicate));
        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(activeService(5L)));
        when(cartItemAddonRepository.findByCartItemId(11L)).thenReturn(List.of());
        when(lexhoUserProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(lexhoAddressRepository.findByUserId(1L)).thenReturn(List.of());
        when(orderRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        CustomerCartResponse response = customerCartService.updateQuantity(1L, request);

        verify(cartItemAddonRepository).deleteByCartItemId(10L);
        verify(customerCartItemRepository).delete(item);
        verify(customerCartItemRepository).save(duplicate);
        assertEquals(3, response.getItems().get(0).getQuantity());
    }

    @Test
    void updateQuantityShouldRejectMergedQuantityAboveConfiguredMaximum() {
        CustomerCartItemEntity item = cartItem(10L, 1L, 5L, 4);
        CustomerCartItemEntity duplicate = cartItem(11L, 1L, 5L, 3);

        CookFamilySizeEntity familySize = new CookFamilySizeEntity();
        familySize.setId(5L);
        familySize.setFamilySize("LARGE");
        familySize.setFamilySizeLabel("Large Family");

        CustomerUpdateCartItemRequest request = new CustomerUpdateCartItemRequest();
        request.setItemId(10L);
        request.setFamilySizeId(5L);

        CartConfigEntity config = new CartConfigEntity();
        config.setMaxServiceQuantity(5);

        when(customerCartItemRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartConfigService.currentEntity()).thenReturn(config);
        when(cookFamilySizeService.requireActive(5L, null)).thenReturn(familySize);
        when(cartItemAddonRepository.findByCartItemId(10L)).thenReturn(List.of());
        when(customerCartItemRepository.findByUserIdAndServiceIdAndAddonSignature(1L, 5L, "FAMILY:LARGE::NONE"))
                .thenReturn(Optional.of(duplicate));

        assertThrows(CodedValidationException.class, () -> customerCartService.updateQuantity(1L, request));
    }

    private static AdminServiceEntity activeService(Long id) {
        AdminServiceEntity service = new AdminServiceEntity();
        setId(service, id);
        service.setActive(true);
        service.setDeleted(false);
        return service;
    }

    private static CustomerAddToCartRequest request(Long serviceId, int quantity) {
        CustomerAddToCartRequest request = new CustomerAddToCartRequest();
        request.setServiceId(serviceId);
        request.setQuantity(quantity);
        return request;
    }

    private static CustomerCartItemEntity cartItem(Long id, Long userId, Long serviceId, Integer quantity) {
        CustomerCartItemEntity item = new CustomerCartItemEntity();
        item.setId(id);
        item.setUserId(userId);
        item.setServiceId(serviceId);
        item.setQuantity(quantity);
        return item;
    }

    private static void setId(AdminServiceEntity service, Long id) {
        try {
            Field field = AdminServiceEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(service, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setCategoryId(AdminServiceEntity service, Long categoryId) {
        try {
            Object category = Class.forName("com.lexho.user_service.admin.entity.AdminCategoryEntity")
                    .getDeclaredConstructor().newInstance();
            Field idField = category.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, categoryId);
            Field categoryField = AdminServiceEntity.class.getDeclaredField("category");
            categoryField.setAccessible(true);
            categoryField.set(service, category);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setId(LexhoAddress address, Long id) {
        try {
            Field field = LexhoAddress.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(address, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static AddonEntity addon(Long id, String name, String price, boolean required, boolean quantityAllowed) {
        AddonEntity addon = new AddonEntity();
        try {
            Field field = AddonEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(addon, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        addon.setAddonName(name);
        addon.setPrice(new BigDecimal(price));
        addon.setRequired(required);
        addon.setQuantityAllowed(quantityAllowed);
        return addon;
    }
}
