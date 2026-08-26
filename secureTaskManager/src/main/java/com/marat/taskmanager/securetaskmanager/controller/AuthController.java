package com.marat.taskmanager.securetaskmanager.controller;

import com.marat.taskmanager.securetaskmanager.dto.auth.*;
import com.marat.taskmanager.securetaskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<String> me(Authentication authentication) {
        return ResponseEntity.ok(authentication.getName());
    }
}