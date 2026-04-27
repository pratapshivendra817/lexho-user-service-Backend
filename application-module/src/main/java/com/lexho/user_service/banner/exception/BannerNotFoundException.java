package com.lexho.user_service.banner.exception;

public class BannerNotFoundException extends RuntimeException {
    public BannerNotFoundException(Long id) {
        super("Banner not found for id: " + id);
    }
}

