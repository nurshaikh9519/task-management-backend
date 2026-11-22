
package com.example.taskManagement.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.taskManagement.repository.UserRepository;
import com.example.taskManagement.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Read Authorization header
        String authHeader = request.getHeader("Authorization");

        // Step 2: Check Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract JWT token
        String token = authHeader.substring(7);

        try {

            // Step 4: Extract email from token
            String email = jwtService.extractUsername(token);

            // Step 5: Check whether user is already authenticated
            if (email != null &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {

                // Step 6: Validate token and check user existence
                if (jwtService.isTokenValid(token, email)
                        && userRepository.existsByEmail(email)) {

                    // Step 7: Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    // Step 8: Store authentication in SecurityContext
                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid or expired token: leave request unauthenticated.
            SecurityContextHolder.clearContext();
        }

        // Step 9: Continue filter chain
        filterChain.doFilter(request, response);
    }
}
