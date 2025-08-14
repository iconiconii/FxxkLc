package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.codetop.enums.AuthProvider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User entity representing system users with authentication and profile information.
 * 
 * Features:
 * - Support for both local and OAuth authentication
 * - User preferences stored as JSON
 * - Timezone support for international users
 * - Audit trail with login tracking
 * - Security role management
 * 
 * @author CodeTop Team
 */
@TableName("users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 50)
    @TableField("username")
    private String username;

    @Email
    @NotBlank
    @Size(max = 100)
    @TableField("email")
    private String email;

    @JsonIgnore
    @Size(max = 255)
    @TableField("password_hash")
    private String passwordHash;

    @TableField("first_name")
    private String firstName;

    @TableField("last_name")
    private String lastName;

    @TableField("avatar_url")
    private String avatarUrl;

    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    @Builder.Default
    @TableField("is_email_verified")
    private Boolean emailVerified = false;

    @TableField("last_login_at")
    private LocalDateTime lastLogin;

    @Builder.Default
    @TableField("timezone")
    private String timezone = "UTC";

    @Builder.Default
    @TableField("oauth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @TableField("oauth_id")
    private String providerId;

    @TableField("preferences")
    private String preferences; // Store as JSON string

    @TableField(exist = false) // This field won't be mapped to database
    private Set<String> roles = new HashSet<>();

    // Note: login_attempts, locked_until, verification_token, reset_token fields
    // are not present in current database schema - removing to match DB
    // @Builder.Default
    // @TableField("login_attempts")
    // private Integer loginAttempts = 0;

    // @TableField("locked_until")
    // private LocalDateTime lockedUntil;

    // @TableField("verification_token")
    // private String verificationToken;

    // @TableField("reset_token")
    // private String resetToken;

    // @TableField("reset_token_expires")
    // private LocalDateTime resetTokenExpires;

    // Derived fields for convenience
    @JsonIgnore
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return username;
    }

    @JsonIgnore
    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }

    @JsonIgnore
    public boolean isOAuthUser() {
        return authProvider != AuthProvider.LOCAL;
    }

    // Commented out account locking since locked_until field doesn't exist in DB
    // public boolean isAccountLocked() {
    //     return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    // }

    /**
     * Check if user has specific role.
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Add role to user.
     */
    public void addRole(String role) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }

    /**
     * Remove role from user.
     */
    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Update last login timestamp.
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
        // Removed loginAttempts and lockedUntil updates since fields don't exist in DB
    }

    /**
     * Get last login time - alias for compatibility.
     * 
     * @JsonIgnore to prevent Redis serialization conflicts
     */
    @JsonIgnore
    public LocalDateTime getLastLoginAt() {
        return this.lastLogin;
    }

    // Commented out login attempts functionality since fields don't exist in DB
    // /**
    //  * Increment login attempts and lock account if necessary.
    //  */
    // public void incrementLoginAttempts() {
    //     this.loginAttempts++;
    //     if (this.loginAttempts >= 5) {
    //         this.lockedUntil = LocalDateTime.now().plusMinutes(30);
    //     }
    // }

    // /**
    //  * Reset login attempts.
    //  */
    // public void resetLoginAttempts() {
    //     this.loginAttempts = 0;
    //     this.lockedUntil = null;
    // }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                ", authProvider=" + authProvider +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}