package com.codetop.dto;

import com.codetop.entity.FSRSCard;
import com.codetop.enums.FSRSState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FSRS review result VO for API responses.
 * 
 * This VO is specifically designed for API responses to avoid
 * exposing internal type information from cached DTOs.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSReviewResultVO {
    private FSRSCard card;
    private LocalDateTime nextReviewTime;
    private Integer intervalDays;
    private FSRSState newState;
    private BigDecimal difficulty;
    private BigDecimal stability;
}