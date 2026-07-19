package com.lexho.user.security;

import com.lexho.user.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    public AuthenticatedUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getDetails() instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Authentication context is unavailable");
        }
        return user;
    }
}
