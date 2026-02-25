package com.flowstudio.auth_service.service;

import com.flowstudio.auth_service.dto.*;
import com.flowstudio.auth_service.entity.RefreshToken;
import com.flowstudio.auth_service.entity.Role;
import com.flowstudio.auth_service.entity.User;
import com.flowstudio.auth_service.repository.RoleRepository;
import com.flowstudio.auth_service.repository.UserRepository;
import com.flowstudio.auth_service.security.JwtProvider;
import com.flowstudio.auth_service.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final RabbitMQPublisher rabbitMQPublisher;

    @Transactional
    public User registerUser(RegisterRequest signUpRequest, boolean isCandidate) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .passwordHash(encoder.encode(signUpRequest.getPassword()))
                .enabled(true)
                .build();

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (isCandidate) {
            Role candidateRole = roleRepository.findByName("CANDIDATE")
                    .orElseThrow(() -> new RuntimeException("Error: Role CANDIDATE is not found."));
            roles.add(candidateRole);
        } else {
            if (strRoles == null || strRoles.isEmpty()) {
                Role defaultRole = roleRepository.findByName("HR")
                        .orElseThrow(() -> new RuntimeException("Error: Default Role HR is not found."));
                roles.add(defaultRole);
            } else {
                strRoles.forEach(role -> {
                    Role mappedRole = roleRepository.findByName(role.toUpperCase())
                            .orElseThrow(() -> new RuntimeException("Error: Role " + role + " is not found."));
                    roles.add(mappedRole);
                });
            }
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Publish to message broker
        try {
            rabbitMQPublisher.publishUserCreatedEvent(savedUser);
        } catch (Exception e) {
            log.error("Failed to publish user.created event for {}", savedUser.getEmail(), e);
        }

        return savedUser;
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt = jwtProvider.generateJwtToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        RefreshToken refreshToken = refreshTokenService
                .createRefreshToken(java.util.UUID.fromString(userDetails.getId()));

        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail()) // Username in UserDetailsImpl is mapped to Email to fix spring security
                                               // flow
                .roles(roles)
                .build();

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .user(userDto)
                .build();
    }

    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    List<String> roles = user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList());
                    String token = jwtProvider.generateTokenFromEmail(user.getEmail(), user.getId().toString(), roles);
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public AuthResponse.UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("User is not authenticated");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return AuthResponse.UserDto.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername()) // This is actually email from UserDetailsImpl
                .email(userDetails.getEmail())
                .roles(roles)
                .build();
    }
}
