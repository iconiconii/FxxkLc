package com.codetop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.cookies")
public class CookieAuthProperties {
    /** Enable cookie-based auth in addition to Authorization header. */
    private boolean enabled = true;
    /** Cookie name for access token. */
    private String accessName = "ACCESS_TOKEN";
    /** Cookie name for refresh token. */
    private String refreshName = "REFRESH_TOKEN";
    /** Cookie Path. */
    private String path = "/";
    /** Cookie Domain (optional). */
    private String domain;
    /** SameSite: Lax/Strict/None */
    private String sameSite = "Lax";
    /** Secure flag (prod true; dev false). */
    private boolean secure = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getAccessName() { return accessName; }
    public void setAccessName(String accessName) { this.accessName = accessName; }
    public String getRefreshName() { return refreshName; }
    public void setRefreshName(String refreshName) { this.refreshName = refreshName; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getSameSite() { return sameSite; }
    public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
}

