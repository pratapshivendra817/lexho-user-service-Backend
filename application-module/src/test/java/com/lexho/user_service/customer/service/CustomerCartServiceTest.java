package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.repository.CustomerCartItemRepository;
import com.lexho.user_service.customer.repository.CustomerCatalogRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CustomerCartServiceTest {

    @Mock
    private CustomerCartItemRepository customerCartItemRepository;
    @Mock
    private CustomerCatalogRepository customerCatalogRepository;

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

        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(customerCartItemRepository.findByUserIdAndServiceId(11L, 5L)).thenReturn(Optional.empty());
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

        when(customerCatalogRepository.findById(5L)).thenReturn(Optional.of(service));
        when(customerCartItemRepository.findByUserIdAndServiceId(11L, 5L)).thenReturn(Optional.of(existing));
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
        when(customerCatalogRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> customerCartService.addToCart(11L, request));
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
}
