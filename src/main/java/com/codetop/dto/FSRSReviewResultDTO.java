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
 * FSRS review result DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSReviewResultDTO {
    private FSRSCard card;
    private LocalDateTime nextReviewTime;
    private Integer intervalDays;
    private FSRSState newState;
    private BigDecimal difficulty;
    private BigDecimal stability;
}