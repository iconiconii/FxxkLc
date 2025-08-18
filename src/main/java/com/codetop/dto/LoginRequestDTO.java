package com.codetop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO for authentication.
 * 
 * @author CodeTop Team
 */
@Data
public class LoginRequestDTO {
    @NotBlank(message = "Email or username is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}