package com.codetop.dto;

import com.codetop.mapper.FSRSCardMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FSRS review queue DTO for API responses and caching.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSReviewQueueDTO {
    private List<FSRSCardMapper.ReviewQueueCard> cards;
    private Integer totalCount;
    private FSRSCardMapper.UserLearningStats stats;
    private LocalDateTime generatedAt;
}