package com.codetop.recommendation.service;

import com.codetop.recommendation.dto.RecommendationFeedbackRequest;
import com.codetop.recommendation.entity.RecommendationFeedback;
import com.codetop.recommendation.mapper.RecommendationFeedbackMapper;
import com.codetop.util.CacheHelper;
import com.codetop.service.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationFeedbackService {
    private final RecommendationFeedbackMapper mapper;
    private final CacheHelper cacheHelper;
    private final UserProfilingService userProfilingService;

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
        
        // Invalidate user profile cache since feedback affects recommendations
        userProfilingService.invalidateUserProfileCache(request.getUserId());
        log.debug("Invalidated user profile cache after recommendation feedback: userId={}", request.getUserId());
    }
}

