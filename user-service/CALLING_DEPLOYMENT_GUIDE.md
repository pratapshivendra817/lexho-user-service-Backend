# WebRTC Signaling Deployment Guide

## Runtime shape

- Spring Boot REST API on `SERVER_PORT`
- Socket.IO signaling server on `SOCKET_IO_PORT`
- MySQL for `call_session` and `call_log`
- Redis optional for distributed rate limiting
- STUN always configured
- TURN configured for relay fallback only

## Required environment

```properties
USER_SERVICE_DB_URL=jdbc:mysql://mysql:3306/lexho_user_service
USER_SERVICE_DB_USERNAME=lexho
USER_SERVICE_DB_PASSWORD=change-me
JWT_SECRET=replace-with-32-byte-minimum-secret
JWT_ISSUER=lexho-user-service
JWT_AUDIENCE=lexho-clients
WEBRTC_STUN_URL=stun:stun.l.google.com:19302
WEBRTC_TURN_URL=turn:turn.example.com:3478?transport=udp
WEBRTC_TURN_USERNAME=turn-user
WEBRTC_TURN_CREDENTIAL=turn-password
REDIS_HOST=redis
REDIS_PORT=6379
SOCKET_IO_HOST=0.0.0.0
SOCKET_IO_PORT=9092
CALL_TIMEOUT_SECONDS=30
```

## Production notes

- Put HTTP and Socket.IO behind the same API gateway or load balancer.
- Enable sticky sessions for Socket.IO if you run multiple instances without Redis-backed message fanout.
- If you scale horizontally, move socket presence and signaling fanout to Redis or another shared pub/sub layer.
- Keep TURN credentials short-lived if the mobile apps can fetch them dynamically.
- Expose only TLS endpoints. WebRTC signaling should run over HTTPS/WSS.
- Monitor:
  - active call count
  - rejected/busy/missed ratio
  - signaling latency
  - socket reconnect rate
  - TURN usage rate

## Call flow

1. Caller connects to Socket.IO with JWT.
2. Caller emits `call:initiate`.
3. Backend validates users, persists `call_session`, emits `call:incoming`.
4. Caller sends `call:offer`.
5. Callee replies with `call:answer`.
6. Both peers exchange `call:ice-candidate`.
7. Media flows peer-to-peer through STUN, then TURN only if required.
8. Backend closes the session on reject, busy, timeout, or end.
