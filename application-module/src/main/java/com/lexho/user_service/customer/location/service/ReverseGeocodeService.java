package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;

public interface ReverseGeocodeService {
    ReverseGeocodeResponse reverse(double latitude, double longitude);
}
