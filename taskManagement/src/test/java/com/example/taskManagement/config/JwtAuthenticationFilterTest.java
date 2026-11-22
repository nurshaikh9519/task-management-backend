
package com.example.taskManagement.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.taskManagement.config.JwtAuthenticationFilter;
import com.example.taskManagement.repository.UserRepository;
import com.example.taskManagement.service.JwtService;

import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtShouldAuthenticateUser() throws ServletException, IOException {

        // ARRANGE

        String email = "nursen@gmail.com";
        String token = "valid-jwt-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(token, email)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // ACT

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(email, authentication.getName());
        assertTrue(authentication.isAuthenticated());

        // VERIFY

        verify(jwtService).extractUsername(token);
        verify(jwtService).isTokenValid(token, email);
        verify(userRepository).existsByEmail(email);
    }
    
    @Test
    void missingJwtShouldNotAuthenticateUser()
            throws ServletException, IOException {

        // ARRANGE

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        // ACT

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // ASSERT

        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        // VERIFY

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }
    

@Test
void invalidJwtShouldNotAuthenticateUser()
        throws ServletException, IOException {

    // ARRANGE

    String token = "invalid-jwt-token";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtService.extractUsername(token))
            .thenThrow(new io.jsonwebtoken.JwtException("Invalid token"));

    // ACT

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // ASSERT

    assertNull(
            SecurityContextHolder.getContext().getAuthentication()
    );

    // VERIFY

    verify(jwtService).extractUsername(token);

    verify(jwtService, never())
            .isTokenValid(anyString(), anyString());

    verifyNoInteractions(userRepository);
}


@Test
void validJwtShouldNotAuthenticateWhenUserDoesNotExist()
        throws ServletException, IOException {

    // ARRANGE

    String email = "unknown@gmail.com";
    String token = "valid-jwt-token";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtService.extractUsername(token))
            .thenReturn(email);

    when(jwtService.isTokenValid(token, email))
            .thenReturn(true);

    when(userRepository.existsByEmail(email))
            .thenReturn(false);

    // ACT

    jwtAuthenticationFilter.doFilter(request, response, filterChain);

    // ASSERT

    assertNull(
            SecurityContextHolder.getContext().getAuthentication()
    );

    // VERIFY

    verify(jwtService).extractUsername(token);
    verify(jwtService).isTokenValid(token, email);
    verify(userRepository).existsByEmail(email);
}
}
