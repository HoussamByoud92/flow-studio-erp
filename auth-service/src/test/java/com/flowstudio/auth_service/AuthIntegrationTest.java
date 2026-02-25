package com.flowstudio.auth_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowstudio.auth_service.dto.LoginRequest;
import com.flowstudio.auth_service.dto.RegisterRequest;
import com.flowstudio.auth_service.dto.TokenRefreshRequest;
import com.flowstudio.auth_service.repository.RefreshTokenRepository;
import com.flowstudio.auth_service.repository.RoleRepository;
import com.flowstudio.auth_service.repository.UserRepository;
import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.service.RabbitMQPublisher;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RabbitMQPublisher rabbitMQPublisher;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        if (roleRepository.findByName("HR").isEmpty()) {
            roleRepository.save(Role.builder().name("HR").build());
        }
        if (roleRepository.findByName("CANDIDATE").isEmpty()) {
            roleRepository.save(Role.builder().name("CANDIDATE").build());
        }
    }

    @Test
    void testAuthenticationFlow() throws Exception {
        // 1. Register candidate
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("johndoe");
        registerReq.setEmail("john@example.com");
        registerReq.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register/candidate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // 2. Login
        LoginRequest loginReq = new LoginRequest("john@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String responseStr = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(responseStr, "$.accessToken");
        String refreshToken = JsonPath.read(responseStr, "$.refreshToken");

        // 3. Access Protected Endpoint
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));

        // 4. Refresh Token
        TokenRefreshRequest refreshReq = new TokenRefreshRequest();
        refreshReq.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }
}
