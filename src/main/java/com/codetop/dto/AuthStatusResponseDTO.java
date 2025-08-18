package com.codetop.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Authentication status response DTO.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
public class AuthStatusResponseDTO {
    private Boolean authenticated;
    private String message;
    private UserInfoDTO user;
}