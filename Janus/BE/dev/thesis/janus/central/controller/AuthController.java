package dev.thesis.janus.central.controller;

import dev.thesis.janus.central.dto.AuthRequestDTO;
import dev.thesis.janus.central.dto.AuthResponse;
import dev.thesis.janus.central.dto.GoogleAuthRequest;
import dev.thesis.janus.central.dto.PasswordResetDTO;
import dev.thesis.janus.central.service.AuthService;
import dev.thesis.janus.central.service.PasswordResetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Api(tags = "Authentication")
public class AuthController {

    private final AuthService authService;
    private static final Logger logger = LogManager.getLogger(AuthController.class);

    @PostMapping("/google/login")
    @ApiOperation("Process user login with Google credentials")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        logger.info("Received authentication request for email: {}", request.getEmail());
        AuthResponse response = authService.authenticateUser(request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/login")
    @ApiOperation("Login with email and password")
    public ResponseEntity<AuthResponse> emailLogin(@Valid @RequestBody AuthRequestDTO.LoginRequest request) {
        logger.info("Received email login request for: {}", request.getEmail());
        AuthResponse response = authService.emailLogin(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @ApiOperation("Sign up with name, email, and password")
    public ResponseEntity<AuthResponse> emailSignup(@Valid @RequestBody AuthRequestDTO.SignupRequest request) {
        logger.info("Received signup request for: {}", request.getEmail());
        AuthResponse response = authService.emailSignup(request.getName(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private final PasswordResetService passwordResetService;
    @PostMapping("/forgot-password")
    @ApiOperation("Request a password reset using email")
    public ResponseEntity<PasswordResetDTO.ResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetDTO.RequestReset request) {

        logger.info("Password reset request received for email: {}", request.getEmail());

        PasswordResetDTO.ResetResponse response = passwordResetService.requestPasswordReset(request.getEmail());

        return ResponseEntity.ok(response);
    }
    @PostMapping("/verify-reset")
    @ApiOperation("Verify if a reset token is valid")
    public ResponseEntity<PasswordResetDTO.VerifyResponse> verifyResetToken(
            @Valid @RequestBody PasswordResetDTO.VerifyToken request) {
        logger.info("Reset token verification received");
       

        String email = passwordResetService.extractEmailFromToken(request.getResetToken());
        logger.info("Email extracted from token: {}", email);

        PasswordResetDTO.VerifyResponse response = passwordResetService.verifyResetToken(
                email,
                request.getResetToken()
        );

        return ResponseEntity.ok(response);
    }
    @PostMapping("/change-password")
    @ApiOperation("Complete password reset using JWT token and new password")
    public ResponseEntity<PasswordResetDTO.ResetResponse> completePasswordReset(
            @Valid @RequestBody PasswordResetDTO.ValidateReset request) {
        logger.info("Password reset completion received");

       

        String email = passwordResetService.extractEmailFromToken(request.getResetToken());
        logger.info("Email extracted from token: {}", email);

        PasswordResetDTO.ResetResponse response = passwordResetService.completePasswordReset(
                email,
                request.getResetToken(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(response);
    }
}