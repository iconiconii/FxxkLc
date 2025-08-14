package com.codetop.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Domain events for cache invalidation system
 * 
 * These events are published when significant domain changes occur,
 * triggering appropriate cache invalidation strategies.
 * 
 * @author CodeTop Team
 */
public class Events {
    
    // Problem Events
    @Data
    @AllArgsConstructor
    public static class ProblemEvent {
        private ProblemEventType type;
        private Long problemId;
        private String title; // Optional for logging
        
        public enum ProblemEventType {
            CREATED, UPDATED, DELETED, STATUS_CHANGED
        }
    }
    
    // User Events
    @Data
    @AllArgsConstructor
    public static class UserEvent {
        private UserEventType type;
        private Long userId;
        private String username; // Optional for logging
        
        public enum UserEventType {
            PROFILE_UPDATED, PROGRESS_UPDATED, PARAMETERS_OPTIMIZED
        }
    }
    
    // Review Events  
    @Data
    @AllArgsConstructor
    public static class ReviewEvent {
        private ReviewEventType type;
        private Long userId;
        private Long problemId;
        private Integer rating; // Optional for logging
        
        public enum ReviewEventType {
            REVIEW_COMPLETED, BULK_REVIEWS
        }
    }
    
    // Company/Organization Events
    @Data
    @AllArgsConstructor  
    public static class CompanyEvent {
        private CompanyEventType type;
        private Long companyId;
        private String companyName; // Optional for logging
        
        public enum CompanyEventType {
            COMPANY_UPDATED, DEPARTMENT_UPDATED, POSITION_UPDATED, FREQUENCY_UPDATED
        }
    }
    
    // Cache Management Events
    @Data
    @AllArgsConstructor
    public static class CacheFlushEvent {
        private CacheScope scope;
        private String reason;
        
        public enum CacheScope {
            ALL, PROBLEMS, USERS, FSRS, CODETOP_FILTER
        }
    }
    
    @Data
    @AllArgsConstructor
    public static class CacheWarmupEvent {
        private WarmupPriority priority;
        
        public enum WarmupPriority {
            HIGH, MEDIUM, LOW
        }
    }
}