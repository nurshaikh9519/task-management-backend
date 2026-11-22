
package com.example.taskManagement.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import com.example.taskManagement.repository.UserRepository;
import com.example.taskManagement.service.JwtService;
import com.example.taskManagement.service.TaskService;
import com.example.taskManagement.service.UserService;

import io.jsonwebtoken.JwtException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = {
	    "spring.datasource.url=jdbc:postgresql://localhost:5432/task_management_test_db",
	    "spring.datasource.username=postgres",
	    "spring.datasource.password=${DB_PASSWORD}",
	    "spring.datasource.driver-class-name=org.postgresql.Driver",
	    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
	    "spring.jpa.hibernate.ddl-auto=create-drop"
	})
	@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserService userService;

    // 1. REGISTRATION SHOULD BE PUBLIC

    @Test
    void registerShouldBeAccessibleWithoutToken() throws Exception {

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // 2. LOGIN SHOULD BE PUBLIC

    @Test
    void loginShouldBeAccessibleWithoutToken() throws Exception {

        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // 3. TASK API SHOULD REJECT REQUEST WITHOUT TOKEN

    @Test
    void taskApiShouldReturnUnauthorizedWithoutToken()
            throws Exception {

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    // 4. PROFILE API SHOULD REJECT REQUEST WITHOUT TOKEN

    @Test
    void profileApiShouldReturnUnauthorizedWithoutToken()
            throws Exception {

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // 5. TASK API SHOULD ACCEPT VALID JWT

    @Test
    void taskApiShouldAllowAccessWithValidToken()
            throws Exception {

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("user@gmail.com");

        when(jwtService.isTokenValid(
                "valid-token", "user@gmail.com"))
                .thenReturn(true);

        when(userRepository.existsByEmail("user@gmail.com"))
                .thenReturn(true);

        when(taskService.searchTasks(
                any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyInt(),
                anyString(), anyString()))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/tasks")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(taskService).searchTasks(
                any(), any(), any(), any(),
                anyBoolean(), anyInt(), anyInt(),
                anyString(), anyString());
    }

    // 6. TASK API SHOULD REJECT INVALID JWT

    @Test
    void taskApiShouldReturnUnauthorizedForInvalidToken()
            throws Exception {

        when(jwtService.extractUsername("invalid-token"))
                .thenReturn("user@gmail.com");

        when(jwtService.isTokenValid(
                "invalid-token", "user@gmail.com"))
                .thenReturn(false);

        mockMvc.perform(get("/tasks")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    // 7. TASK API SHOULD REJECT MALFORMED JWT

    @Test
    void taskApiShouldReturnUnauthorizedForMalformedToken()
            throws Exception {

        when(jwtService.extractUsername("malformed-token"))
                .thenThrow(new JwtException("Invalid JWT"));

        mockMvc.perform(get("/tasks")
                .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }
}
