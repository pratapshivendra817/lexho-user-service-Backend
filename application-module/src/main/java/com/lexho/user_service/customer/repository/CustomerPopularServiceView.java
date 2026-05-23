package com.lexho.user_service.customer.repository;

import java.math.BigDecimal;

public interface CustomerPopularServiceView {
    Long getId();
    String getName();
    String getImageUrl();
    BigDecimal getPrice();
    Double getRating();
    Long getCategoryId();
}
