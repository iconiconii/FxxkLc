package com.codetop.util;

import com.codetop.entity.User;
import com.codetop.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility for token generation and validation.
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;
    
    private final TokenBlacklistService tokenBlacklistService;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate access token for user.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles());
        claims.put("type", "access");

        return createToken(claims, user.getUsername(), expiration);
    }

    /**
     * Generate refresh token for user.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        return createToken(claims, user.getUsername(), refreshExpiration);
    }

    /**
     * Create JWT token with claims.
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate access token.
     */
    public boolean validateAccessToken(String token) {
        try {
            // First check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }
            
            Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get("type");
            return "access".equals(tokenType) && !isTokenExpired(claims);
        } catch (Exception e) {
            log.debug("Invalid access token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate refresh token.
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = (String) claims.get("type");
            return "refresh".equals(tokenType) && !isTokenExpired(claims);
        } catch (Exception e) {
            log.debug("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get user ID from access token.
     */
    public Long getUserIdFromAccessToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Get user ID from refresh token.
     */
    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Get username from token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Get expiration date from token.
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get access token expiration in milliseconds.
     */
    public Long getAccessTokenExpiration() {
        return expiration;
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public Long getRefreshTokenExpiration() {
        return refreshExpiration;
    }

    /**
     * Extract claims from token.
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired.
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}