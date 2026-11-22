
package com.example.taskManagement.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import com.example.taskManagement.dto.LoginResponse;
import com.example.taskManagement.dto.UserResponse;
import com.example.taskManagement.exception.InvalidCredentialsException;
import com.example.taskManagement.exception.ResourceNotFoundException;
import com.example.taskManagement.service.UserService;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void registerUserShouldReturnCreatedResponse() throws Exception {

        // ARRANGE

        UserResponse response = new UserResponse(
                1L,
                "Nursen",
                "nursen@gmail.com"
        );

        when(userService.registerUser(any()))
                .thenReturn(response);

        String requestJson = """
                {
                    "name": "Nursen",
                    "email": "nursen@gmail.com",
                    "password": "Password@123"
                }
                """;

        // ACT & ASSERT

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nursen"))
                .andExpect(jsonPath("$.email")
                        .value("nursen@gmail.com"));

        // VERIFY

        verify(userService).registerUser(any());
    }
    

@Test
void loginUserShouldReturnSuccessResponse() throws Exception {

    // ARRANGE

    LoginResponse response = new LoginResponse(
            1L,
            "Nursen",
            "nursen@gmail.com",
            "Login successful",
            "mock-jwt-token"
    );

    when(userService.loginUser(any()))
            .thenReturn(response);

    String requestJson = """
            {
                "email": "nursen@gmail.com",
                "password": "Password@123"
            }
            """;

    // ACT & ASSERT

    mockMvc.perform(post("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Nursen"))
            .andExpect(jsonPath("$.email")
                    .value("nursen@gmail.com"))
            .andExpect(jsonPath("$.message")
                    .value("Login successful"))
            .andExpect(jsonPath("$.token")
                    .value("mock-jwt-token"));

    // VERIFY

    verify(userService).loginUser(any());
}


@Test
void getMyProfileShouldReturnUserDetails() throws Exception {

    // ARRANGE

    UserResponse response = new UserResponse(
            1L,
            "Nursen",
            "nursen@gmail.com"
    );

    when(userService.getMyProfile())
            .thenReturn(response);

    // ACT & ASSERT

    mockMvc.perform(get("/users/me"))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Nursen"))
            .andExpect(jsonPath("$.email")
                    .value("nursen@gmail.com"));

    // VERIFY

    verify(userService).getMyProfile();
}


@Test
void updateProfileShouldReturnUpdatedUserDetails() throws Exception {

    // ARRANGE

    UserResponse response = new UserResponse(
            1L,
            "Nursen Shaikh",
            "nursen.shaikh@gmail.com"
    );

    when(userService.updateProfile(any()))
            .thenReturn(response);

    String requestJson = """
            {
                "name": "Nursen Shaikh",
                "email": "nursen.shaikh@gmail.com"
            }
            """;

    // ACT & ASSERT

    mockMvc.perform(put("/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name")
                    .value("Nursen Shaikh"))
            .andExpect(jsonPath("$.email")
                    .value("nursen.shaikh@gmail.com"));

    // VERIFY

    verify(userService).updateProfile(any());
}


@Test
void changePasswordShouldReturnSuccessMessage() throws Exception {

    // ARRANGE

    String requestJson = """
            {
                "currentPassword": "OldPassword@123",
                "newPassword": "NewPassword@123"
            }
            """;

    // ACT & ASSERT

    mockMvc.perform(patch("/users/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message")
                    .value("Password changed successfully"));

    // VERIFY

    verify(userService).changePassword(any());
}


@Test
void registerUserShouldReturnBadRequestForInvalidEmail()
        throws Exception {

    // ARRANGE

    String requestJson = """
            {
                "name": "Nursen",
                "email": "invalid-email",
                "password": "Password@123"
            }
            """;

    // ACT & ASSERT

    mockMvc.perform(post("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                    .value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.email")
                    .exists());

    // VERIFY

    verifyNoInteractions(userService);
}


@Test
void registerUserShouldReturnBadRequestWhenPasswordIsMissing()
        throws Exception {

    // ARRANGE

    String requestJson = """
            {
                "name": "Nursen",
                "email": "nursen@gmail.com",
                "password": ""
            }
            """;

    // ACT & ASSERT

    mockMvc.perform(post("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                    .value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.password")
                    .exists());

    // VERIFY

    verifyNoInteractions(userService);
}


@Test
void loginUserShouldReturnUnauthorizedForInvalidCredentials()
        throws Exception {

    // ARRANGE

    String requestJson = """
            {
                "email": "nursen@gmail.com",
                "password": "WrongPassword@123"
            }
            """;

    when(userService.loginUser(any()))
            .thenThrow(
                    new InvalidCredentialsException(
                            "Invalid email or password"
                    )
            );

    // ACT & ASSERT

    mockMvc.perform(post("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))

            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error")
                    .value("Invalid email or password"));

    // VERIFY

    verify(userService).loginUser(any());
}

@Test
void registerUserShouldReturnBadRequestWhenEmailAlreadyExists()
        throws Exception {

    String requestJson = """
            {
                "name": "Nursen",
                "email": "nursen@gmail.com",
                "password": "Password@123"
            }
            """;

    when(userService.registerUser(any()))
            .thenThrow(
                    new IllegalArgumentException(
                            "Email is already registered"
                    )
            );

    mockMvc.perform(post("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                    .value("Email is already registered"))
            .andExpect(jsonPath("$.path")
                    .value("/users/register"));

    verify(userService).registerUser(any());
}

@Test
void changePasswordShouldReturnUnauthorizedForIncorrectCurrentPassword()
        throws Exception {

    String requestJson = """
            {
                "currentPassword": "WrongPassword@123",
                "newPassword": "NewPassword@123"
            }
            """;

    doThrow(new InvalidCredentialsException(
            "Current password is incorrect"))
            .when(userService).changePassword(any());

    mockMvc.perform(patch("/users/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error")
                    .value("Current password is incorrect"));

    verify(userService).changePassword(any());
}

@Test
void updateProfileShouldReturnBadRequestWhenEmailAlreadyExists()
        throws Exception {

    String requestJson = """
            {
                "name": "Nursen Shaikh",
                "email": "existing@gmail.com"
            }
            """;

    when(userService.updateProfile(any()))
            .thenThrow(
                    new IllegalArgumentException(
                            "Email is already registered"
                    )
            );

    mockMvc.perform(put("/users/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                    .value("Email is already registered"))
            .andExpect(jsonPath("$.path").value("/users/me"));

    verify(userService).updateProfile(any());
}

@Test
void getMyProfileShouldReturnNotFoundWhenUserDoesNotExist()
        throws Exception {

    when(userService.getMyProfile())
            .thenThrow(
                    new ResourceNotFoundException(
                            "User not found"
                    )
            );

    mockMvc.perform(get("/users/me"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("User not found"))
            .andExpect(jsonPath("$.path").value("/users/me"));

    verify(userService).getMyProfile();
}
}
