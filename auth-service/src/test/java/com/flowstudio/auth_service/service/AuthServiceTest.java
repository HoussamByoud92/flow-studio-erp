package com.flowstudio.auth_service.service;

import com.flowstudio.auth_service.dto.AuthResponse;
import com.flowstudio.auth_service.dto.LoginRequest;
import com.flowstudio.auth_service.dto.RegisterRequest;
import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.entity.User;
import com.flowstudio.auth_service.repository.RefreshTokenRepository;
import com.flowstudio.auth_service.repository.RoleRepository;
import com.flowstudio.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        if (roleRepository.findByName("HR").isEmpty()) {
            roleRepository.save(new Role(null, "HR", Set.of()));
        }

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@test.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest("test@test.com", "password123");
    }

    @Test
    void testRegisterUser_Success() {
        User result = authService.registerUser(registerRequest, false);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertTrue(userRepository.existsByUsername("testuser"));
    }

    @Test
    void testRegisterUser_DuplicateUsername() {
        authService.registerUser(registerRequest, false);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("testuser");
        duplicate.setEmail("other@test.com");
        duplicate.setPassword("password123");

        Exception ex = assertThrows(RuntimeException.class, () -> authService.registerUser(duplicate, false));
        assertEquals("Error: Username is already taken!", ex.getMessage());
    }

    @Test
    void testRegisterUser_DuplicateEmail() {
        authService.registerUser(registerRequest, false);

        RegisterRequest duplicate = new RegisterRequest();
        duplicate.setUsername("otheruser");
        duplicate.setEmail("test@test.com");
        duplicate.setPassword("password123");

        Exception ex = assertThrows(RuntimeException.class, () -> authService.registerUser(duplicate, false));
        assertEquals("Error: Email is already in use!", ex.getMessage());
    }

    @Test
    void testAuthenticateUser() {
        // Register first
        authService.registerUser(registerRequest, false);

        // Authenticate
        AuthResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("test@test.com", response.getUser().getEmail());
    }
}
