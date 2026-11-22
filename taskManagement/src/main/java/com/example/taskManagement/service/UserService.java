
package com.example.taskManagement.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.taskManagement.dto.UserRequest;
import com.example.taskManagement.dto.UserResponse;
import com.example.taskManagement.entity.User;
import com.example.taskManagement.repository.UserRepository;
import com.example.taskManagement.dto.ChangePasswordRequest;
import com.example.taskManagement.dto.LoginRequest;
import com.example.taskManagement.dto.LoginResponse;
import com.example.taskManagement.dto.UpdateProfileRequest;
import com.example.taskManagement.exception.InvalidCredentialsException;
import com.example.taskManagement.exception.ResourceNotFoundException;
import com.example.taskManagement.service.JwtService;
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse registerUser(UserRequest request) {

        // Check whether email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        // Create new user
        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Encrypt password using BCrypt
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Save user
        User savedUser = userRepository.save(user);

        // Return response DTO
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }
    
    public LoginResponse loginUser(LoginRequest request) {

        // Step 1: Find user using email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                    new InvalidCredentialsException("Invalid email or password"));

        // Step 2: Verify entered password against BCrypt hash
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        // Step 3: Reject incorrect password
        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }
        String token = jwtService.generateToken(user.getEmail());
        // Step 4: Return successful login response
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                "Login successful",
                token
        );
    }
    
    public UserResponse getMyProfile() {

        User user = getCurrentUser();

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    } 
    
    public UserResponse updateProfile(UpdateProfileRequest request) {

        User user = getCurrentUser();

        String newEmail = request.getEmail().trim().toLowerCase();

        // Check whether another user already has this email
        User existingUser = userRepository.findByEmail(newEmail)
                .orElse(null);

        if (existingUser != null
                && !existingUser.getId().equals(user.getId())) {

            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        user.setName(request.getName().trim());
        user.setEmail(newEmail);

        User updatedUser = userRepository.save(user);

        return new UserResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail()
        );
    }
    
    public void changePassword(ChangePasswordRequest request) {

        User user = getCurrentUser();

        // Verify current password
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash())) {

            throw new InvalidCredentialsException(
                    "Current password is incorrect"
            );
        }

        // Prevent setting the same password again
        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        // Store the new BCrypt hash
        user.setPasswordHash(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);
    }
    
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }
}
