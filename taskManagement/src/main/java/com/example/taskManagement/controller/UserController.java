
package com.example.taskManagement.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.taskManagement.dto.UserRequest;
import com.example.taskManagement.dto.UserResponse;
import com.example.taskManagement.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import com.example.taskManagement.dto.ChangePasswordRequest;
import com.example.taskManagement.dto.LoginRequest;
import com.example.taskManagement.dto.LoginResponse;
import com.example.taskManagement.dto.UpdateProfileRequest;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody UserRequest request) {

        UserResponse response =
                userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.loginUser(request);

        return ResponseEntity.ok(response);
    }
    
    @SecurityRequirement(name = "bearerAuth")
	@GetMapping("/me")
	public ResponseEntity<UserResponse> getMyProfile() {
	
	    return ResponseEntity.ok(userService.getMyProfile());
	}
	
    @SecurityRequirement(name = "bearerAuth")
	@PutMapping("/me")
	public ResponseEntity<UserResponse> updateProfile(
	        @Valid @RequestBody UpdateProfileRequest request) {
	
	    return ResponseEntity.ok(userService.updateProfile(request));
	}
	
    @SecurityRequirement(name = "bearerAuth")
	@PatchMapping("/me/password")
	public ResponseEntity<Map<String, String>> changePassword(
	        @Valid @RequestBody ChangePasswordRequest request) {
	
	    userService.changePassword(request);
	
	    return ResponseEntity.ok(
	            Map.of("message", "Password changed successfully")
	    );
	}

}
