package com.lexho.user.exception;

public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String role) {
        super("Invalid role: " + role + ". Allowed roles are CUSTOMER, PROVIDER");
    }
}
