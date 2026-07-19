package com.lexho.user.security;

import com.lexho.common.enums.Role;

public record AuthenticatedUser(Long userId, String subject, Role role) {
}
