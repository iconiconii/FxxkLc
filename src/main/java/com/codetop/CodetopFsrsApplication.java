package com.codetop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for CodeTop FSRS Backend System.
 * 
 * This application provides:
 * - Intelligent spaced repetition scheduling using FSRS algorithm
 * - Comprehensive problem management for algorithm practice
 * - User progress tracking and analytics
 * - Social features including leaderboards and achievements
 * 
 * Features enabled:
 * - MyBatis-Plus for optimized database operations
 * - Manual Redis caching with RedisTemplate for performance optimization
 * - Async processing for parameter optimization
 * - Scheduled tasks for background processing
 * 
 * @author CodeTop Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan({"com.codetop.mapper","com.codetop.recommendation.mapper"})
public class CodetopFsrsApplication {

    /**
     * Main method to start the Spring Boot application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CodetopFsrsApplication.class, args);
    }
}
