package com.lexho.user.socket;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
@ConditionalOnProperty(name = "socket.io.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SocketIoProperties.class)
public class SocketIoConfig {

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer(SocketIoProperties properties, SocketAuthService socketAuthService) {
        com.corundumstudio.socketio.Configuration configuration = new com.corundumstudio.socketio.Configuration();
        configuration.setHostname(properties.getHost());
        configuration.setPort(properties.getPort());
        configuration.setAuthorizationListener(authorizationListener(socketAuthService));
        SocketIOServer socketIOServer = new SocketIOServer(configuration);
        socketIOServer.start();
        return socketIOServer;
    }

    private AuthorizationListener authorizationListener(SocketAuthService socketAuthService) {
        return handshakeData -> {
            socketAuthService.authenticate(handshakeData);
            return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
        };
    }
}
