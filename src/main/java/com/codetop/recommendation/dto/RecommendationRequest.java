package com.codetop.recommendation.dto;

import com.codetop.recommendation.service.LearningObjective;
import com.codetop.recommendation.service.DifficultyPreference;
import com.codetop.recommendation.service.RecommendationType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for recommendation API calls
 */
@Data
@Builder
public class RecommendationRequest {
    private Long userId;
    private Integer limit;
    private LearningObjective objective;
    private List<String> domains;
    private DifficultyPreference difficultyPreference;
    private Integer timebox;
    private RecommendationType requestedType;
    private String abGroup;
    private Boolean forceRefresh;
}