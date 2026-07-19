package com.lexho.user.socket;

import com.lexho.common.enums.Role;

public record SocketPrincipal(Long userId, Role role, String subject) {
}
