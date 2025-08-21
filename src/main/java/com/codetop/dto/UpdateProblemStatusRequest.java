package com.codetop.dto;

import lombok.Data;

/**
 * Request DTO for updating problem status.
 * 
 * @author CodeTop Team
 */
@Data
public class UpdateProblemStatusRequest {
    private String status;
    private Integer mastery;
    private String notes;
    
    /**
     * 幂等性请求ID，用于防止重复更新问题状态
     */
    private String requestId;
}