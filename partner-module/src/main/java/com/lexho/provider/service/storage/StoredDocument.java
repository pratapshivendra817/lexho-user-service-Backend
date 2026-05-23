package com.lexho.partner.service.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoredDocument {
    private String path;
    private String fileName;
    private String contentType;
}

