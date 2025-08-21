package com.codetop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Register request DTO for user registration.
 * 
 * @author CodeTop Team
 */
@Data
public class RegisterRequestDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    private String firstName;
    private String lastName;
    private String timezone;
    
    /**
     * 幂等性请求ID，用于防止重复注册
     */
    private String requestId;
}