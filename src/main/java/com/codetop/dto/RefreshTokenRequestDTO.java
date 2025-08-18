package com.codetop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token request DTO.
 * 
 * @author CodeTop Team
 */
@Data
public class RefreshTokenRequestDTO {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}