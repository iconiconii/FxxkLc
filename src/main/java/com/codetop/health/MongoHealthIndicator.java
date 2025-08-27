package com.codetop.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component("mongoHealthChecker")
public class MongoHealthIndicator {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public MongoHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public boolean isHealthy() {
        try {
            String collectionName = "health_check";
            
            Map<String, Object> healthDoc = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", "health_check"
            );
            
            mongoTemplate.insert(healthDoc, collectionName);
            
            long count = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("status").is("health_check")
                ), 
                collectionName
            );
            
            mongoTemplate.remove(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("status").is("health_check")
                ), 
                collectionName
            );
            
            log.info("MongoDB health check passed. Database: {}, Collections: {}", 
                mongoTemplate.getDb().getName(), mongoTemplate.getCollectionNames().size());
            return true;
                
        } catch (Exception e) {
            log.error("MongoDB health check failed", e);
            return false;
        }
    }
    
    public Map<String, Object> getHealthInfo() {
        try {
            return Map.of(
                "database", mongoTemplate.getDb().getName(),
                "collections", mongoTemplate.getCollectionNames().size(),
                "status", "UP",
                "timestamp", LocalDateTime.now()
            );
        } catch (Exception e) {
            return Map.of(
                "status", "DOWN",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
}