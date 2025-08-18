package com.codetop.dto;

import com.codetop.entity.User;
import com.codetop.enums.AuthProvider;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Cache DTO for Redis serialization.
 * 
 * This DTO contains only the essential user information needed for authentication
 * and avoids the serialization issues with complex entity objects.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class UserCacheDTO {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private boolean isActive;
    private boolean isEmailVerified;
    private LocalDateTime lastLoginAt;
    private String timezone;
    private AuthProvider authProvider;
    private Set<String> roles;
    
    @JsonCreator
    public UserCacheDTO(
            @JsonProperty("id") Long id,
            @JsonProperty("username") String username,
            @JsonProperty("email") String email,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("avatarUrl") String avatarUrl,
            @JsonProperty("isActive") boolean isActive,
            @JsonProperty("isEmailVerified") boolean isEmailVerified,
            @JsonProperty("lastLoginAt") LocalDateTime lastLoginAt,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("authProvider") AuthProvider authProvider,
            @JsonProperty("roles") Set<String> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatarUrl = avatarUrl;
        this.isActive = isActive;
        this.isEmailVerified = isEmailVerified;
        this.lastLoginAt = lastLoginAt;
        this.timezone = timezone;
        this.authProvider = authProvider;
        this.roles = roles != null ? roles : new HashSet<>();
    }
    
    /**
     * Convert User entity to UserCacheDTO.
     */
    public static UserCacheDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        
        return UserCacheDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .timezone(user.getTimezone())
                .authProvider(user.getAuthProvider())
                .roles(user.getRoles() != null ? new HashSet<>(user.getRoles()) : new HashSet<>())
                .build();
    }
    
    /**
     * Convert UserCacheDTO to User entity.
     */
    public User toUser() {
        User user = User.builder()
                .username(this.username)
                .email(this.email)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .avatarUrl(this.avatarUrl)
                .isActive(this.isActive)
                .emailVerified(this.isEmailVerified)
                .lastLogin(this.lastLoginAt)
                .timezone(this.timezone)
                .authProvider(this.authProvider)
                .build();
        
        // Set the ID after building (since it's inherited from BaseEntity)
        user.setId(this.id);
        
        // Set the roles
        if (this.roles != null && !this.roles.isEmpty()) {
            this.roles.forEach(user::addRole);
        } else {
            // Ensure default USER role
            user.addRole("USER");
        }
        
        return user;
    }
    
    /**
     * Add role to the cached user.
     */
    public void addRole(String role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
    }
    
    /**
     * Check if user has specific role.
     */
    public boolean hasRole(String role) {
        return this.roles != null && this.roles.contains(role);
    }
}