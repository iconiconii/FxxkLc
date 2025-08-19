package com.codetop.dto;

import com.codetop.mapper.FSRSCardMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FSRS review queue VO for API responses.
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
public class FSRSReviewQueueVO {
    private List<FSRSCardMapper.ReviewQueueCard> cards;
    private Integer totalCount;
    private FSRSCardMapper.UserLearningStats stats;
    private LocalDateTime generatedAt;
}