package com.codetop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Optimized FSRS review queue VO for API responses with pagination support.
 * 
 * This VO is specifically designed for frontend consumption,
 * containing only essential fields to reduce payload size.
 * 
 * @author CodeTop Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FSRSReviewQueueVO {
    private List<ReviewQueueCardVO> cards;
    private Integer totalCount;
    private LearningStatsVO stats;
    private LocalDateTime generatedAt;
    
    // Pagination fields
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;
}