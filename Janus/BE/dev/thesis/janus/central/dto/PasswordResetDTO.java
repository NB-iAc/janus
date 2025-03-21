package dev.thesis.janus.central.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordResetDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestReset {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateReset {
        @NotBlank(message = "Reset token is required")
        private String resetToken;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyToken {
        @NotBlank(message = "Reset token is required")
        private String resetToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyResponse {
        private boolean valid;
        private String message;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetResponse {
        private boolean success;
        private String message;
        private String resetToken;
    }
}