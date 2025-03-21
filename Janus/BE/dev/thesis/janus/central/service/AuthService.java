package dev.thesis.janus.central.service;

import dev.thesis.janus.central.dto.AuthResponse;
import dev.thesis.janus.central.dto.GoogleAuthRequest;
import dev.thesis.janus.central.dto.UserDTO;
import dev.thesis.janus.central.exception.AuthenticationException;
import dev.thesis.janus.central.model.User;
import dev.thesis.janus.central.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${application.default.profile-picture}")
    private String defaultProfilePicture;

    @Transactional
    public AuthResponse authenticateUser(GoogleAuthRequest request) {
        logger.info("Processing authentication for user with email: {}", request.getEmail());

        try {
           

            if (request.getGoogle_id() == null || request.getEmail() == null) {
                logger.error("Missing required fields in authentication request");
                throw new AuthenticationException("Missing required fields");
            }

           

            User user = userRepository.findByGoogleId(request.getGoogle_id()).orElse(null);

           

            if (user == null) {
                logger.info("Creating new user with email: {}", request.getEmail());
                user = new User();
                user.setEmail(request.getEmail());
                user.setUsername(request.getName());
                user.setGoogleId(request.getGoogle_id());
                user.setPictureUrl(request.getPicture());

               

                user.setUsertoken(UUID.randomUUID().toString());

                user = userRepository.save(user);
                logger.info("Created new user with ID: {}", user.getUserid());
            }

           

            String token = jwtService.generateToken(user);

           

            UserDTO userDTO = convertToDTO(user);
            AuthResponse response = new AuthResponse(token, userDTO);

            logger.info("Authentication successful for user ID: {}", user.getUserid());
            return response;

        } catch (Exception e) {
            if (!(e instanceof AuthenticationException)) {
                logger.error("Error during authentication", e);
                throw new AuthenticationException("Authentication failed: " + e.getMessage());
            }
            throw e;
        }
    }

    @Transactional
    public AuthResponse emailSignup(String name, String email, String password) {
       

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            throw new AuthenticationException("User already exists with this email");
        }

       

        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setUsertoken(UUID.randomUUID().toString());

       

        String hashedPassword = passwordEncoder.encode(password);
        user.setUserdetails(hashedPassword);

       

        user.setPictureUrl(defaultProfilePicture);

       

        User savedUser = userRepository.save(user);

       

        String token = jwtService.generateToken(savedUser);

       

        UserDTO userDTO = convertToDTO(savedUser);

        return new AuthResponse(token, userDTO);
    }

    @Transactional
    public AuthResponse emailLogin(String email, String password) {
       

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found"));

       

        if (!passwordEncoder.matches(password, user.getUserdetails())) {
            throw new AuthenticationException("Incorrect password");
        }

       

        String token = jwtService.generateToken(user);

       

        UserDTO userDTO = convertToDTO(user);

        return new AuthResponse(token, userDTO);
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getUserid(),
                user.getUsername(),
                user.getEmail(),
                user.getUsertoken(),
                user.getUserdetails(),
                user.getPictureUrl()
        );
    }
}