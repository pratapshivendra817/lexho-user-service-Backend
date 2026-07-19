package com.lexho.user.socket;

import com.corundumstudio.socketio.HandshakeData;
import com.lexho.common.enums.Role;
import com.lexho.user.security.AuthenticatedUser;
import com.lexho.user.security.JwtService;
import org.springframework.stereotype.Component;

@Component
public class SocketAuthService {

    private final JwtService jwtService;

    public SocketAuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public SocketPrincipal authenticate(HandshakeData handshakeData) {
        String token = extractToken(handshakeData);
        AuthenticatedUser user = jwtService.parse(token);
        if (user.role() != Role.CUSTOMER && user.role() != Role.PARTNER) {
            throw new IllegalArgumentException("Unsupported role");
        }
        return new SocketPrincipal(user.userId(), user.role(), user.subject());
    }

    private String extractToken(HandshakeData handshakeData) {
        String header = handshakeData.getHttpHeaders().get("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        String queryToken = handshakeData.getSingleUrlParam("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        throw new IllegalArgumentException("Socket token is required");
    }
}
