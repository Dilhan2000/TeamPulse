package com.shan.weeklyreport.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie factory for access and refresh tokens (C1-T08).
 * Ensures HttpOnly, SameSite=Lax, and configurable Secure attributes.
 */
@Component
public class AuthCookieFactory {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api";

    private final boolean secure;

    public AuthCookieFactory(@Value("${app.cookie.secure:false}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie createAccessTokenCookie(String token, Duration maxAge) {
        return buildCookie(ACCESS_TOKEN_COOKIE, token, maxAge);
    }

    public ResponseCookie createRefreshTokenCookie(String token, Duration maxAge) {
        return buildCookie(REFRESH_TOKEN_COOKIE, token, maxAge);
    }

    public ResponseCookie createClearAccessTokenCookie() {
        return buildCookie(ACCESS_TOKEN_COOKIE, "", Duration.ZERO);
    }

    public ResponseCookie createClearRefreshTokenCookie() {
        return buildCookie(REFRESH_TOKEN_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }

    private Optional<String> extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(val -> val != null && !val.isBlank())
                .findFirst();
    }
}
