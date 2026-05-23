package com.lexho.partner.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("Document not found for id: " + id);
    }
}

