package com.codetop.enums;

/**
 * Authentication provider enumeration for user login methods.
 * 
 * Supported providers:
 * - LOCAL: Traditional email/password authentication
 * - GITHUB: GitHub OAuth2 authentication
 * - GOOGLE: Google OAuth2 authentication
 * 
 * @author CodeTop Team
 */
public enum AuthProvider {
    LOCAL,
    GITHUB,
    GOOGLE
}