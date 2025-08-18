package com.codetop.dto;

import lombok.Builder;
import lombok.Data;

/**
 * User info DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class UserInfoDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String timezone;
    private String authProvider;
    private Boolean emailVerified;

    public static UserInfoDTO from(com.codetop.entity.User user) {
        return UserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .timezone(user.getTimezone())
                .authProvider(user.getAuthProvider().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}