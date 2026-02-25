package com.flowstudio.auth_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", "mySecretKeyWhichIsLongEnough32Bytes++");
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", 3600000L); // 1 hour
        jwtProvider.init();
    }

    @Test
    void testGenerateAndValidateJwtToken() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        UserDetailsImpl userDetails = new UserDetailsImpl("1", "testuser", "test@test.com", "pass", true, authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        String token = jwtProvider.generateJwtToken(auth);

        assertNotNull(token);
        assertTrue(jwtProvider.validateJwtToken(token));
        assertEquals("test@test.com", jwtProvider.getUserEmailFromJwtToken(token));
        assertEquals("1", jwtProvider.getUserIdFromJwtToken(token));
    }

    @Test
    void testGenerateTokenFromEmail() {
        String token = jwtProvider.generateTokenFromEmail("test2@test.com", "2", List.of("HR"));
        assertNotNull(token);
        assertTrue(jwtProvider.validateJwtToken(token));
        assertEquals("test2@test.com", jwtProvider.getUserEmailFromJwtToken(token));
        assertEquals("2", jwtProvider.getUserIdFromJwtToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        assertFalse(jwtProvider.validateJwtToken("invalid.token.string"));
    }

    @Test
    void testValidateToken_Expired() throws InterruptedException {
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", 1L); // 1 ms expiration
        jwtProvider.init();

        String token = jwtProvider.generateTokenFromEmail("exp@test.com", "3", List.of("ADMIN"));

        Thread.sleep(10); // Wait for expiration

        assertFalse(jwtProvider.validateJwtToken(token));
    }
}
