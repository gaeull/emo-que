package com.emoque.controller;

import com.emoque.config.GoogleOAuthProperties;
import com.emoque.dto.GoogleLoginRequest;
import com.emoque.dto.LoginResponse;
import com.emoque.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthProperties googleProps;

    public AuthController(AuthService authService, GoogleOAuthProperties googleProps) {
        this.authService = authService;
        this.googleProps = googleProps;
    }

    @GetMapping("/google/client-id")
    public ResponseEntity<?> clientId() {
        return ResponseEntity.ok(java.util.Map.of("clientId", authService.getGoogleClientId()));
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> loginGoogle(@Valid @RequestBody GoogleLoginRequest body) {
        return ResponseEntity.ok(authService.loginWithGoogleIdToken(body.idToken()));
    }
}

