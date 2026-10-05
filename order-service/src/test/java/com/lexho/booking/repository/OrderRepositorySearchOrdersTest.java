package com.lexho.booking.repository;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderRepositorySearchOrdersTest {

    private final OrderRepository repository = mock(OrderRepository.class, CALLS_REAL_METHODS);

    @Test
    void searchOrdersShouldAvoidBindingNullableConvertedStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> expected = new PageImpl<>(List.of());
        when(repository.searchOrdersWithoutStatus("abc", pageable)).thenReturn(expected);

        Page<Order> actual = repository.searchOrders("abc", null, pageable);

        assertSame(expected, actual);
        verify(repository).searchOrdersWithoutStatus("abc", pageable);
        verify(repository, never()).searchOrdersByStatus("abc", null, pageable);
    }

    @Test
    void searchOrdersShouldUseStatusQueryWhenStatusIsPresent() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> expected = new PageImpl<>(List.of());
        when(repository.searchOrdersByStatus("abc", OrderStatus.CONFIRMED, pageable)).thenReturn(expected);

        Page<Order> actual = repository.searchOrders("abc", OrderStatus.CONFIRMED, pageable);

        assertSame(expected, actual);
        verify(repository).searchOrdersByStatus("abc", OrderStatus.CONFIRMED, pageable);
        verify(repository, never()).searchOrdersWithoutStatus("abc", pageable);
    }
}
