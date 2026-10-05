package com.lexho.user_service.localization.config;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class LocalizedApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final PartnerLocalizationService localizationService;

    public LocalizedApiResponseAdvice(PartnerLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse && isPartnerApi(request)) {
            apiResponse.setMessage(localizationService.translateMessage(apiResponse.getMessage()));
        }
        return body;
    }

    private boolean isPartnerApi(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/api/partner/") || path.startsWith("/api/v1/partner/");
    }
}
