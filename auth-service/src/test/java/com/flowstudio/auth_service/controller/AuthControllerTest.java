package com.flowstudio.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowstudio.auth_service.dto.*;
import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.entity.User;
import com.flowstudio.auth_service.repository.RefreshTokenRepository;
import com.flowstudio.auth_service.repository.RoleRepository;
import com.flowstudio.auth_service.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setup() {
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();
                if (roleRepository.findByName("HR").isEmpty()) {
                        roleRepository.save(new Role(null, "HR", Set.of()));
                }
                if (roleRepository.findByName("CANDIDATE").isEmpty()) {
                        roleRepository.save(new Role(null, "CANDIDATE", Set.of()));
                }
        }

        @Test
        void testRegisterUser_Success() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setUsername("testuser");
                request.setEmail("test@test.com");
                request.setPassword("password123");

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(content().string("User registered successfully!"));
        }

        @Test
        void testRegisterUser_Failure() throws Exception {
                // First user
                User user = new User();
                user.setUsername("testuser");
                user.setEmail("test@test.com");
                user.setPasswordHash(passwordEncoder.encode("password123"));
                userRepository.save(user);

                // Attempt duplicate
                RegisterRequest request = new RegisterRequest();
                request.setUsername("testuser2");
                request.setEmail("test@test.com");
                request.setPassword("password123");

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Error: Email is already in use!"));
        }

        @Test
        void testRegisterCandidate_Success() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setUsername("candidate");
                request.setEmail("candidate@test.com");
                request.setPassword("password123");

                mockMvc.perform(post("/api/v1/auth/register/candidate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(content().string("Candidate registered successfully!"));
        }

        @Test
        void testAuthenticateUser_Success() throws Exception {
                // Create user
                RegisterRequest regReq = new RegisterRequest();
                regReq.setUsername("loginuser");
                regReq.setEmail("login@test.com");
                regReq.setPassword("password123");
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regReq)));

                // Login
                LoginRequest request = new LoginRequest("login@test.com", "password123");

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        void testRefreshToken_Success() throws Exception {
                // Create user and login
                RegisterRequest regReq = new RegisterRequest();
                regReq.setUsername("refreshuser");
                regReq.setEmail("refresh@test.com");
                regReq.setPassword("password123");
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regReq)));

                LoginRequest loginReq = new LoginRequest("refresh@test.com", "password123");
                MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq))).andReturn();

                String response = result.getResponse().getContentAsString();
                String refreshToken = JsonPath.read(response, "$.refreshToken");

                // Refresh token
                TokenRefreshRequest request = new TokenRefreshRequest();
                request.setRefreshToken(refreshToken);

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        void testGetCurrentUser_Success() throws Exception {
                // Create user and login
                RegisterRequest regReq = new RegisterRequest();
                regReq.setUsername("meuser");
                regReq.setEmail("me@test.com");
                regReq.setPassword("password123");
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regReq)));

                LoginRequest loginReq = new LoginRequest("me@test.com", "password123");
                MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginReq))).andReturn();

                String response = result.getResponse().getContentAsString();
                String accessToken = JsonPath.read(response, "$.accessToken");

                // Get me
                mockMvc.perform(get("/api/v1/auth/me")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("me@test.com"));
        }
}
