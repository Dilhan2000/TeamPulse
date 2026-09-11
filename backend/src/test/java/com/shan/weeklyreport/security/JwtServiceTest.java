package com.shan.weeklyreport.security;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String VALID_SECRET = "super-secret-key-that-is-at-least-32-chars-long-123456";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, 15, 7);
        jwtService.init();
    }

    @Test
    @DisplayName("Startup fails if JWT secret is shorter than 32 characters")
    void shouldFailIfSecretTooShort() {
        JwtService shortSecretService = new JwtService("short-secret", 15, 7);
        assertThrows(IllegalStateException.class, shortSecretService::init);
    }

    @Test
    @DisplayName("Generates valid access token and correctly parses claims")
    void shouldGenerateAndParseAccessToken() {
        User user = new User("Alice Tester", "alice@example.com", "hash", Role.TEAM_MEMBER, AccountStatus.ACTIVE);
        user.setId(42L);

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = jwtService.parseAndValidateAccessToken(token);
        assertEquals("42", claims.getSubject());
        assertEquals("alice@example.com", claims.get("email"));
        assertEquals("TEAM_MEMBER", claims.get("role"));
    }

    @Test
    @DisplayName("Throws JwtException when parsing invalid or tampered token")
    void shouldThrowOnInvalidToken() {
        assertThrows(JwtException.class, () -> jwtService.parseAndValidateAccessToken("garbage.token.here"));
    }

    @Test
    @DisplayName("Generates random opaque refresh token")
    void shouldGenerateOpaqueRefreshToken() {
        String token1 = jwtService.generateRefreshTokenValue();
        String token2 = jwtService.generateRefreshTokenValue();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() >= 32);
    }

    @Test
    @DisplayName("Generates consistent SHA-256 hash for token")
    void shouldHashTokenConsistently() {
        String token = "sample-token-123";
        String hash1 = jwtService.hashToken(token);
        String hash2 = jwtService.hashToken(token);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex string is 64 characters
    }
}
