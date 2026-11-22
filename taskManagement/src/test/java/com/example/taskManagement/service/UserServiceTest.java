
package com.example.taskManagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.taskManagement.dto.ChangePasswordRequest;
import com.example.taskManagement.dto.LoginRequest;
import com.example.taskManagement.dto.LoginResponse;
import com.example.taskManagement.dto.UpdateProfileRequest;
import com.example.taskManagement.dto.UserRequest;
import com.example.taskManagement.dto.UserResponse;
import com.example.taskManagement.entity.User;
import com.example.taskManagement.exception.InvalidCredentialsException;
import com.example.taskManagement.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	
	private BCryptPasswordEncoder realPasswordEncoder;

	@BeforeEach
	void setUp() {
	    realPasswordEncoder = new BCryptPasswordEncoder();
	}

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserShouldRegisterUserSuccessfully() {

        // ARRANGE

        UserRequest request = new UserRequest();
        request.setName("Nursen");
        request.setEmail("nursen@gmail.com");
        request.setPassword("Password@123");

        when(userRepository.existsByEmail("nursen@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password@123"))
                .thenReturn("encrypted-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        // ACT

        UserResponse response = userService.registerUser(request);

        // ASSERT

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Nursen", response.getName());
        assertEquals("nursen@gmail.com", response.getEmail());

        // Verify repository and password encoder interactions

        verify(userRepository).existsByEmail("nursen@gmail.com");

        verify(passwordEncoder).encode("Password@123");

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Nursen", savedUser.getName());
        assertEquals("nursen@gmail.com", savedUser.getEmail());

        // Ensure raw password is not stored

        assertEquals("encrypted-password", savedUser.getPasswordHash());
        assertNotEquals("Password@123", savedUser.getPasswordHash());
    }
    

@Test
void registerUserShouldThrowExceptionWhenEmailAlreadyExists() {

    // ARRANGE

    UserRequest request = new UserRequest();
    request.setName("Nursen");
    request.setEmail("nursen@gmail.com");
    request.setPassword("Password@123");

    when(userRepository.existsByEmail("nursen@gmail.com"))
            .thenReturn(true);

    // ACT & ASSERT

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(request)
    );

    assertEquals(
            "Email is already registered",
            exception.getMessage()
    );

    // VERIFY

    verify(userRepository).existsByEmail("nursen@gmail.com");

    verify(userRepository, never()).save(any(User.class));

    verify(passwordEncoder, never()).encode(anyString());
}


@Test
void registerUserShouldStoreEncryptedPassword() {

    // ARRANGE

    UserRequest request = new UserRequest();
    request.setName("Nursen");
    request.setEmail("nursen@gmail.com");
    request.setPassword("Password@123");

    when(userRepository.existsByEmail("nursen@gmail.com"))
            .thenReturn(false);

    // Use actual BCrypt encryption
    when(passwordEncoder.encode("Password@123"))
            .thenAnswer(invocation ->
                    realPasswordEncoder.encode(
                            invocation.getArgument(0)
                    )
            );

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

    // ACT

    userService.registerUser(request);

    // ASSERT

    ArgumentCaptor<User> userCaptor =
            ArgumentCaptor.forClass(User.class);

    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();

    String storedPassword = savedUser.getPasswordHash();

    assertNotNull(storedPassword);

    // Original password must not be stored
    assertNotEquals("Password@123", storedPassword);

    // BCrypt must be able to match the original password
    assertTrue(
            realPasswordEncoder.matches(
                    "Password@123",
                    storedPassword
            )
    );
}


@Test
void loginUserShouldLoginSuccessfullyWithValidCredentials() {

    // ARRANGE

    LoginRequest request = new LoginRequest();
    request.setEmail("nursen@gmail.com");
    request.setPassword("Password@123");

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail("nursen@gmail.com");
    user.setPasswordHash("encrypted-password");

    when(userRepository.findByEmail("nursen@gmail.com"))
            .thenReturn(Optional.of(user));

    when(passwordEncoder.matches(
            "Password@123",
            "encrypted-password"
    )).thenReturn(true);

    when(jwtService.generateToken("nursen@gmail.com"))
            .thenReturn("mock-jwt-token");

    // ACT

    LoginResponse response = userService.loginUser(request);

    // ASSERT

    assertNotNull(response);

    assertEquals(1L, response.getId());
    assertEquals("Nursen", response.getName());
    assertEquals("nursen@gmail.com", response.getEmail());

    assertEquals("Login successful", response.getMessage());

    assertEquals("mock-jwt-token", response.getToken());

    // VERIFY

    verify(userRepository).findByEmail("nursen@gmail.com");

    verify(passwordEncoder).matches(
            "Password@123",
            "encrypted-password"
    );

    verify(jwtService).generateToken("nursen@gmail.com");
}


@Test
void loginUserShouldThrowExceptionWhenPasswordIsIncorrect() {

    // ARRANGE

    LoginRequest request = new LoginRequest();
    request.setEmail("nursen@gmail.com");
    request.setPassword("WrongPassword@123");

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail("nursen@gmail.com");
    user.setPasswordHash("encrypted-password");

    when(userRepository.findByEmail("nursen@gmail.com"))
            .thenReturn(Optional.of(user));

    when(passwordEncoder.matches(
            "WrongPassword@123",
            "encrypted-password"
    )).thenReturn(false);

    // ACT & ASSERT

    InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> userService.loginUser(request)
    );

    assertEquals(
            "Invalid email or password",
            exception.getMessage()
    );

    // VERIFY

    verify(userRepository).findByEmail("nursen@gmail.com");

    verify(passwordEncoder).matches(
            "WrongPassword@123",
            "encrypted-password"
    );

    verify(jwtService, never()).generateToken(anyString());
}

@Test
void loginUserShouldThrowExceptionWhenEmailDoesNotExist() {

    // ARRANGE

    LoginRequest request = new LoginRequest();
    request.setEmail("unknown@gmail.com");
    request.setPassword("Password@123");

    when(userRepository.findByEmail("unknown@gmail.com"))
            .thenReturn(Optional.empty());

    // ACT & ASSERT

    assertThrows(
            InvalidCredentialsException.class,
            () -> userService.loginUser(request)
    );

    // VERIFY

    verify(passwordEncoder, never())
            .matches(anyString(), anyString());

    verify(jwtService, never())
            .generateToken(anyString());
}


@Test
void getMyProfileShouldReturnLoggedInUserDetails() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("encrypted-password");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    // ACT

    UserResponse response = userService.getMyProfile();

    // ASSERT

    assertNotNull(response);

    assertEquals(1L, response.getId());
    assertEquals("Nursen", response.getName());
    assertEquals(email, response.getEmail());

    // VERIFY

    verify(userRepository).findByEmail(email);
}


@Test
void updateProfileShouldUpdateUserDetailsSuccessfully() {

    // ARRANGE

    String currentEmail = "nursen@gmail.com";
    String newEmail = "nursen.shaikh@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    currentEmail, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail(currentEmail);
    user.setPasswordHash("encrypted-password");

    when(userRepository.findByEmail(currentEmail))
            .thenReturn(Optional.of(user));

    // No other user has the new email
    when(userRepository.findByEmail(newEmail))
            .thenReturn(Optional.empty());

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setName(" Nursen Shaikh ");
    request.setEmail(" Nursen.Shaikh@Gmail.com ");

    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    // ACT

    UserResponse response = userService.updateProfile(request);

    // ASSERT

    assertNotNull(response);

    assertEquals(1L, response.getId());
    assertEquals("Nursen Shaikh", response.getName());
    assertEquals(newEmail, response.getEmail());

    // VERIFY

    verify(userRepository).findByEmail(currentEmail);
    verify(userRepository).findByEmail(newEmail);
    verify(userRepository).save(user);
}


@Test
void updateProfileShouldThrowExceptionWhenEmailBelongsToAnotherUser() {

    // ARRANGE

    String currentEmail = "nursen@gmail.com";
    String existingEmail = "another@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    currentEmail, null, java.util.List.of()
            )
    );

    User currentUser = new User();
    currentUser.setId(1L);
    currentUser.setName("Nursen");
    currentUser.setEmail(currentEmail);

    User anotherUser = new User();
    anotherUser.setId(2L);
    anotherUser.setName("Another User");
    anotherUser.setEmail(existingEmail);

    when(userRepository.findByEmail(currentEmail))
            .thenReturn(Optional.of(currentUser));

    when(userRepository.findByEmail(existingEmail))
            .thenReturn(Optional.of(anotherUser));

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.setName("Nursen Shaikh");
    request.setEmail(existingEmail);

    // ACT & ASSERT

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateProfile(request)
    );

    assertEquals(
            "Email is already registered",
            exception.getMessage()
    );

    // VERIFY

    verify(userRepository, never()).save(any(User.class));
}


@Test
void changePasswordShouldUpdatePasswordSuccessfully() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("old-encrypted-password");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("OldPassword@123");
    request.setNewPassword("NewPassword@123");

    when(passwordEncoder.matches(
            "OldPassword@123",
            "old-encrypted-password"
    )).thenReturn(true);

    when(passwordEncoder.matches(
            "NewPassword@123",
            "old-encrypted-password"
    )).thenReturn(false);

    when(passwordEncoder.encode("NewPassword@123"))
            .thenReturn("new-encrypted-password");

    // ACT

    userService.changePassword(request);

    // ASSERT

    assertEquals(
            "new-encrypted-password",
            user.getPasswordHash()
    );

    assertNotEquals(
            "old-encrypted-password",
            user.getPasswordHash()
    );

    // VERIFY

    verify(passwordEncoder).encode("NewPassword@123");
    verify(userRepository).save(user);
}

@Test
void changePasswordShouldThrowExceptionWhenCurrentPasswordIsIncorrect() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("old-encrypted-password");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("WrongPassword@123");
    request.setNewPassword("NewPassword@123");

    when(passwordEncoder.matches(
            "WrongPassword@123",
            "old-encrypted-password"
    )).thenReturn(false);

    // ACT & ASSERT

    InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> userService.changePassword(request)
    );

    assertEquals(
            "Current password is incorrect",
            exception.getMessage()
    );

    // VERIFY

    verify(passwordEncoder, never())
            .encode(anyString());

    verify(userRepository, never()).save(any(User.class));
}

@Test
void changePasswordShouldRejectSamePassword() {

    // ARRANGE

    String email = "nursen@gmail.com";

    SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                    email, null, java.util.List.of()
            )
    );

    User user = new User();
    user.setId(1L);
    user.setName("Nursen");
    user.setEmail(email);
    user.setPasswordHash("old-encrypted-password");

    when(userRepository.findByEmail(email))
            .thenReturn(Optional.of(user));

    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setCurrentPassword("OldPassword@123");
    request.setNewPassword("OldPassword@123");

    when(passwordEncoder.matches(
            "OldPassword@123",
            "old-encrypted-password"
    )).thenReturn(true);

    // ACT & ASSERT

    IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(request)
    );

    assertEquals(
            "New password must be different from current password",
            exception.getMessage()
    );

    // VERIFY

    verify(passwordEncoder, never())
            .encode(anyString());

    verify(userRepository, never()).save(any(User.class));
}
}
