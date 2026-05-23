package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
