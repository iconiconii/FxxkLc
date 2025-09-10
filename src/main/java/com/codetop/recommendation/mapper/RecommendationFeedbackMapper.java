package com.codetop.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codetop.recommendation.entity.RecommendationFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface RecommendationFeedbackMapper extends BaseMapper<RecommendationFeedback> {
}

