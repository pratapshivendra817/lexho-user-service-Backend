package com.lexho.user_service.lexhopartner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;

final class PartnerAttachmentUrlSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_ATTACHMENT_URL_LENGTH = 500;

    private PartnerAttachmentUrlSupport() {
    }

    static String serializeAttachmentUrls(List<String> attachmentUrls) {
        List<String> sanitized = sanitizeAttachmentUrls(attachmentUrls);
        if (sanitized.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Invalid attachmentUrls");
        }
    }

    static List<String> deserializeAttachmentUrls(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = OBJECT_MAPPER.readValue(rawValue, STRING_LIST_TYPE);
            return values == null ? List.of() : values;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    static List<String> sanitizeAttachmentUrls(List<String> attachmentUrls) {
        if (attachmentUrls == null || attachmentUrls.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : attachmentUrls) {
            String normalized = value == null || value.isBlank() ? null : value.trim();
            if (normalized == null) {
                continue;
            }
            if (normalized.length() > MAX_ATTACHMENT_URL_LENGTH) {
                throw new BadRequestException("attachmentUrl is too long");
            }
            if (!sanitized.contains(normalized)) {
                sanitized.add(normalized);
            }
            if (sanitized.size() > MAX_ATTACHMENTS) {
                throw new BadRequestException("No more than 10 attachments are allowed");
            }
        }
        return sanitized;
    }
}
