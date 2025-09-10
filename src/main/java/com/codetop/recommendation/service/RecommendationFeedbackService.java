package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.RecommendationFeedbackRequest;
import com.codetop.recommendation.entity.RecommendationFeedback;
import com.codetop.recommendation.mapper.RecommendationFeedbackMapper;
import com.codetop.util.CacheHelper;
import com.codetop.service.CacheKeyBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RecommendationFeedbackService {
    private final RecommendationFeedbackMapper mapper;
    private final CacheHelper cacheHelper;

    public RecommendationFeedbackService(RecommendationFeedbackMapper mapper, CacheHelper cacheHelper) {
        this.mapper = mapper;
        this.cacheHelper = cacheHelper;
    }

    public void submit(Long problemId, RecommendationFeedbackRequest request) {
        RecommendationFeedback fb = new RecommendationFeedback();
        fb.setUserId(request.getUserId());
        fb.setProblemId(problemId);
        fb.setFeedback(request.getFeedback());
        fb.setNote(request.getNote());
        fb.setCreatedAt(LocalDateTime.now());
        mapper.insert(fb);

        // Evict recommendation cache for this user
        String pattern = CacheKeyBuilder.buildUserDomainPattern("rec-ai", request.getUserId());
        cacheHelper.evictByPattern(pattern);
    }
}

