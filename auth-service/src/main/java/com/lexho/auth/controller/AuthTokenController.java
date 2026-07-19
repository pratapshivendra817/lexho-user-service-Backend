package com.lexho.auth.controller;

import com.lexho.auth.dto.IssueTokenRequest;
import com.lexho.auth.dto.RefreshTokenRequest;
import com.lexho.auth.dto.TokenPairResponse;
import com.lexho.auth.service.AuthTokenService;
import com.lexho.auth.support.AuthRequestSupport;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class AuthTokenController {

    private final AuthTokenService authTokenService;

    public AuthTokenController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenPairResponse> issue(@Valid @RequestBody IssueTokenRequest request) {
        return ResponseEntity.ok(authTokenService.issueTokens(
                request.getUserId(),
                AuthRequestSupport.parseRole(request.getRole())
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authTokenService.refreshTokens(
                AuthRequestSupport.normalizeRefreshToken(request.getRefreshToken())
        ));
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RefreshTokenRequest request) {
        authTokenService.revokeRefreshToken(AuthRequestSupport.normalizeRefreshToken(request.getRefreshToken()));
        return ResponseEntity.noContent().build();
    }
}
