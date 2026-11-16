package com.smartdesk.ticket_service.controller;

import com.smartdesk.ticket_service.dto.request.EmailOtpRequest;
import com.smartdesk.ticket_service.dto.request.LoginRequest;
import com.smartdesk.ticket_service.dto.request.RegisterRequest;
import com.smartdesk.ticket_service.dto.request.VerifyOtpRequest;
import com.smartdesk.ticket_service.dto.response.AuthResponse;
import com.smartdesk.ticket_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Registration
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Registration successful");
    }

    // Login using email and password
    @PostMapping("/login/password")
    public ResponseEntity<AuthResponse> loginWithPassword(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.loginWithPassword(request)
        );
    }

    // Request OTP using email only
    @PostMapping("/login/otp")
    public ResponseEntity<String> requestLoginOtp(
            @Valid @RequestBody EmailOtpRequest request) {

        return ResponseEntity.ok(
                authService.requestLoginOtp(request)
        );
    }

    // Verify OTP and generate JWT
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        return ResponseEntity.ok(
                authService.verifyOtp(request)
        );
    }

    // Legacy endpoint for older frontend versions
    @PostMapping("/login")
    public ResponseEntity<String> legacyLogin(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }
}