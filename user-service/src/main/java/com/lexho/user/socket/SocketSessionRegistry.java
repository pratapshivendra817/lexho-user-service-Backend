package com.lexho.user.socket;

import com.corundumstudio.socketio.SocketIOClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketSessionRegistry {

    private final Map<Long, Map<UUID, SocketIOClient>> userSockets = new ConcurrentHashMap<>();
    private final Map<UUID, SocketPrincipal> principals = new ConcurrentHashMap<>();

    public void register(SocketIOClient client, SocketPrincipal principal) {
        principals.put(client.getSessionId(), principal);
        userSockets.computeIfAbsent(principal.userId(), ignored -> new ConcurrentHashMap<>())
                .put(client.getSessionId(), client);
    }

    public void unregister(SocketIOClient client) {
        SocketPrincipal principal = principals.remove(client.getSessionId());
        if (principal == null) {
            return;
        }
        Map<UUID, SocketIOClient> sockets = userSockets.get(principal.userId());
        if (sockets == null) {
            return;
        }
        sockets.remove(client.getSessionId());
        if (sockets.isEmpty()) {
            userSockets.remove(principal.userId());
        }
    }

    public SocketPrincipal requirePrincipal(SocketIOClient client) {
        SocketPrincipal principal = principals.get(client.getSessionId());
        if (principal == null) {
            throw new IllegalStateException("Socket principal not found");
        }
        return principal;
    }

    public List<SocketIOClient> getUserSockets(Long userId) {
        Map<UUID, SocketIOClient> sockets = userSockets.get(userId);
        return sockets == null ? List.of() : new ArrayList<>(sockets.values());
    }
}
