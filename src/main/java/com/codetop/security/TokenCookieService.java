package com.codetop.security;

import com.codetop.config.CookieAuthProperties;
import com.codetop.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCookieService {

    private final CookieAuthProperties props;
    private final JwtUtil jwtUtil;

    public void writeAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        if (!props.isEnabled()) return;
        try {
            long accessMaxAge = Math.max(1, jwtUtil.getAccessTokenExpiration() / 1000); // seconds
            long refreshMaxAge = Math.max(1, jwtUtil.getRefreshTokenExpiration() / 1000);

            ResponseCookie access = buildCookie(props.getAccessName(), accessToken, accessMaxAge);
            ResponseCookie refresh = buildCookie(props.getRefreshName(), refreshToken, refreshMaxAge);

            response.addHeader("Set-Cookie", access.toString());
            response.addHeader("Set-Cookie", refresh.toString());
        } catch (Exception e) {
            log.warn("Failed to write auth cookies: {}", e.getMessage());
        }
    }

    public void clearAuthCookies(HttpServletResponse response) {
        if (!props.isEnabled()) return;
        ResponseCookie access = buildCookie(props.getAccessName(), "", 0);
        ResponseCookie refresh = buildCookie(props.getRefreshName(), "", 0);
        response.addHeader("Set-Cookie", access.toString());
        response.addHeader("Set-Cookie", refresh.toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.isSecure())
                .sameSite(props.getSameSite())
                .path(props.getPath())
                .maxAge(maxAgeSeconds);
        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            b.domain(props.getDomain());
        }
        return b.build();
    }
}
