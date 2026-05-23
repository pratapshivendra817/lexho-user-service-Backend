package com.lexho.user_service.admin.repository;

import java.math.BigDecimal;

public interface PopularServiceView {
    Long getId();
    String getName();
    String getImageUrl();
    BigDecimal getPrice();
    Double getRating();
    Integer getPriority();
}
