package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.PasswordResetDTO;
import dev.thesis.janus.central.exception.AuthenticationException;
import dev.thesis.janus.central.exception.ResourceNotFoundException;
import dev.thesis.janus.central.model.User;
import dev.thesis.janus.central.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    

    public String extractEmailFromToken(String resetToken) {
        try {
            return jwtService.validatePasswordResetToken(resetToken);
        } catch (Exception e) {
            logger.warn("Failed to extract email from token: {}", e.getMessage());
            throw new AuthenticationException("Invalid reset token");
        }
    }

    

    public PasswordResetDTO.VerifyResponse verifyResetToken(String email, String resetToken) {
        logger.info("Verifying reset token for email: {}", email);

        try {
           

            String tokenEmail = jwtService.validatePasswordResetToken(resetToken);

           

            if (!email.toLowerCase().equals(tokenEmail.toLowerCase())) {
                logger.warn("Email mismatch in verification. Token email: {}, Provided email: {}",
                        tokenEmail, email);
                return new PasswordResetDTO.VerifyResponse(false, "Email mismatch", null);
            }

           

            String emailLowercase = email.toLowerCase();
            boolean userExists = userRepository.findAll().stream()
                    .anyMatch(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(emailLowercase));

            if (!userExists) {
                logger.warn("User not found during reset token verification: {}", email);
                return new PasswordResetDTO.VerifyResponse(false, "User not found", null);
            }

            return new PasswordResetDTO.VerifyResponse(true, "Token is valid", email);

        } catch (ExpiredJwtException e) {
            logger.warn("Expired reset token verified for email: {}", email);
            return new PasswordResetDTO.VerifyResponse(false, "Reset token has expired", null);

        } catch (Exception e) {
            logger.warn("Invalid reset token verified for email: {}", email);
            return new PasswordResetDTO.VerifyResponse(false, "Invalid reset token", null);
        }
    }

    

    public PasswordResetDTO.ResetResponse requestPasswordReset(String email) {
        logger.info("Password reset requested for email: {}", email);

       

        String emailLowercase = email.toLowerCase();
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(emailLowercase))
                .findFirst()
                .orElseThrow(() -> {
                    logger.warn("Password reset requested for non-existent email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });

       

        String resetToken = jwtService.generatePasswordResetToken(email);

        logger.info("Password reset token created for user: {}", user.getUserid());

       

       

        return new PasswordResetDTO.ResetResponse(
                true,
                "Password reset request processed successfully. Check your email for instructions.",
                resetToken
        );
    }

    

    @Transactional
    public PasswordResetDTO.ResetResponse completePasswordReset(String email, String resetToken, String newPassword) {
        logger.info("Password reset completion attempt for email: {}", email);

        try {
           

            String tokenEmail = jwtService.validatePasswordResetToken(resetToken);

           

            if (!email.toLowerCase().equals(tokenEmail.toLowerCase())) {
                logger.warn("Email mismatch in password reset attempt. Token email: {}, Provided email: {}",
                        tokenEmail, email);
                throw new AuthenticationException("Email mismatch in reset request");
            }

           

            String emailLowercase = email.toLowerCase();
            User user = userRepository.findAll().stream()
                    .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().equals(emailLowercase))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

           

            String hashedPassword = passwordEncoder.encode(newPassword);
            user.setUserdetails(hashedPassword);
            userRepository.save(user);

            logger.info("Password reset successful for user: {}", user.getUserid());

            return new PasswordResetDTO.ResetResponse(
                    true,
                    "Password has been reset successfully",
                    null
            );

        } catch (ExpiredJwtException e) {
            logger.warn("Expired reset token used for email: {}", email);
            throw new AuthenticationException("Reset token has expired");

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid reset token used for email: {}", email);
            throw new AuthenticationException("Invalid reset token");
        }
    }
}