package com.codetop.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication response DTO for API responses.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfoDTO user;
}