
package com.example.taskManagement.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            Base64.getEncoder().encodeToString(
                    new byte[32]
            );

    private static final long EXPIRATION = 86400000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
    }

    @Test
    void generateTokenShouldGenerateValidJwtToken() {

        // ACT
        String token = jwtService.generateToken("nursen@gmail.com");

        // ASSERT
        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(token.split("\\.").length == 3);
    }
    
    @Test
    void extractUsernameShouldReturnEmailFromToken() {

        // ARRANGE
        String email = "nursen@gmail.com";

        String token = jwtService.generateToken(email);

        // ACT
        String extractedEmail = jwtService.extractUsername(token);

        // ASSERT
        assertEquals(email, extractedEmail);
    }
    
    @Test
    void isTokenValidShouldReturnTrueForValidToken() {

        // ARRANGE
        String email = "nursen@gmail.com";

        String token = jwtService.generateToken(email);

        // ACT
        boolean result = jwtService.isTokenValid(token, email);

        // ASSERT
        assertTrue(result);
    }
    
    @Test
    void isTokenValidShouldReturnFalseForDifferentEmail() {

        // ARRANGE
        String token = jwtService.generateToken("nursen@gmail.com");

        // ACT
        boolean result = jwtService.isTokenValid(
                token,
                "another@gmail.com"
        );

        // ASSERT
        assertFalse(result);
    }
    
    @Test
    void isTokenValidShouldReturnFalseForInvalidToken() {

        // ARRANGE
        String invalidToken = "invalid.jwt.token";

        // ACT
        boolean result = jwtService.isTokenValid(
                invalidToken,
                "nursen@gmail.com"
        );

        // ASSERT
        assertFalse(result);
    }
    
    @Test
    void isTokenValidShouldReturnFalseForExpiredToken()
            throws InterruptedException {

        // ARRANGE
        JwtService shortExpiryJwtService =
                new JwtService(SECRET, 1);

        String email = "nursen@gmail.com";

        String token = shortExpiryJwtService.generateToken(email);

        // Wait until token expires
        Thread.sleep(100);

        // ACT
        boolean result = shortExpiryJwtService.isTokenValid(token, email);

        // ASSERT
        assertFalse(result);
    }
    
    
}
