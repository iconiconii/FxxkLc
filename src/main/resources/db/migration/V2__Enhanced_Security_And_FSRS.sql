-- =============================================================================
-- CodeTop FSRS Backend - Enhanced Security and FSRS Optimization Migration
-- Version: 2.0.0
-- Purpose: Add comprehensive security logging, enhanced FSRS personalization,
--          and production-ready performance optimizations
-- =============================================================================

-- =============================================================================
-- 1. Enhanced User Parameters Table
-- =============================================================================

-- Drop existing user_parameters table if it exists (will be recreated with enhanced structure)
DROP TABLE IF EXISTS user_parameters;

-- Create enhanced user_fsrs_parameters table with complete FSRS v4.5+ support
CREATE TABLE user_fsrs_parameters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Parameter set ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    
    -- FSRS v4.5+ Parameters (17 weights as individual columns for performance)
    w0 DECIMAL(10,8) NOT NULL DEFAULT 0.40000000 COMMENT 'Initial difficulty weight for Again rating',
    w1 DECIMAL(10,8) NOT NULL DEFAULT 0.60000000 COMMENT 'Initial difficulty weight for Hard rating',
    w2 DECIMAL(10,8) NOT NULL DEFAULT 2.40000000 COMMENT 'Initial difficulty weight for Good rating',
    w3 DECIMAL(10,8) NOT NULL DEFAULT 5.80000000 COMMENT 'Initial difficulty weight for Easy rating',
    w4 DECIMAL(10,8) NOT NULL DEFAULT 4.93000000 COMMENT 'Difficulty decay factor',
    w5 DECIMAL(10,8) NOT NULL DEFAULT 0.94000000 COMMENT 'Difficulty multiplier for Again',
    w6 DECIMAL(10,8) NOT NULL DEFAULT 0.86000000 COMMENT 'Difficulty multiplier for Hard',
    w7 DECIMAL(10,8) NOT NULL DEFAULT 0.01000000 COMMENT 'Difficulty multiplier for Good',
    w8 DECIMAL(10,8) NOT NULL DEFAULT 1.49000000 COMMENT 'Difficulty multiplier for Easy',
    w9 DECIMAL(10,8) NOT NULL DEFAULT 0.14000000 COMMENT 'Initial stability for Again',
    w10 DECIMAL(10,8) NOT NULL DEFAULT 0.94000000 COMMENT 'Initial stability for Hard',
    w11 DECIMAL(10,8) NOT NULL DEFAULT 2.18000000 COMMENT 'Initial stability for Good',
    w12 DECIMAL(10,8) NOT NULL DEFAULT 0.05000000 COMMENT 'Stability multiplier for Again',
    w13 DECIMAL(10,8) NOT NULL DEFAULT 0.34000000 COMMENT 'Stability multiplier for Hard',
    w14 DECIMAL(10,8) NOT NULL DEFAULT 1.26000000 COMMENT 'Stability multiplier for Good',
    w15 DECIMAL(10,8) NOT NULL DEFAULT 0.29000000 COMMENT 'Stability multiplier for Easy',
    w16 DECIMAL(10,8) NOT NULL DEFAULT 2.61000000 COMMENT 'Retrievability threshold',
    
    -- Additional FSRS Configuration
    request_retention DECIMAL(5,4) NOT NULL DEFAULT 0.9000 COMMENT 'Target retention rate (0.7-0.95)',
    maximum_interval INTEGER NOT NULL DEFAULT 36500 COMMENT 'Maximum interval in days',
    easy_bonus DECIMAL(5,4) NOT NULL DEFAULT 1.3000 COMMENT 'Easy answer bonus multiplier',
    hard_interval DECIMAL(5,4) NOT NULL DEFAULT 1.2000 COMMENT 'Hard answer interval multiplier',
    new_interval DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT 'New card interval multiplier',
    graduating_interval INTEGER NOT NULL DEFAULT 1 COMMENT 'Graduating interval in days',
    
    -- Optimization Metadata
    review_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of reviews used for optimization',
    optimization_accuracy DECIMAL(5,4) COMMENT 'Prediction accuracy percentage',
    optimization_method VARCHAR(50) DEFAULT 'DEFAULT' COMMENT 'Optimization algorithm used',
    optimization_iterations INTEGER COMMENT 'Number of optimization iterations',
    optimization_loss DECIMAL(10,6) COMMENT 'Final loss value from optimization',
    learning_rate DECIMAL(10,6) COMMENT 'Learning rate used in optimization',
    regularization DECIMAL(10,6) COMMENT 'Regularization parameter',
    convergence_threshold DECIMAL(10,8) COMMENT 'Convergence threshold used',
    
    -- Performance Tracking
    performance_improvement DECIMAL(5,2) COMMENT 'Percentage improvement over default parameters',
    confidence_score DECIMAL(5,4) COMMENT 'Statistical confidence in parameters',
    
    -- Status and Control
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether parameters are currently active',
    is_optimized BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether parameters are user-optimized',
    version VARCHAR(20) NOT NULL DEFAULT 'FSRS-4.5' COMMENT 'FSRS algorithm version',
    
    -- Timestamps
    last_optimized TIMESTAMP NULL COMMENT 'When parameters were last optimized',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Constraints
    UNIQUE KEY uk_user_active (user_id, is_active),
    CHECK (request_retention >= 0.7 AND request_retention <= 0.95),
    CHECK (maximum_interval >= 1 AND maximum_interval <= 36500),
    CHECK (review_count >= 0),
    
    -- Indexes for Performance
    INDEX idx_user_id (user_id),
    INDEX idx_active (is_active),
    INDEX idx_optimized (is_optimized),
    INDEX idx_last_optimized (last_optimized),
    INDEX idx_review_count (review_count),
    INDEX idx_version (version),
    
    -- Composite indexes for queries
    INDEX idx_user_active_optimized (user_id, is_active, is_optimized),
    INDEX idx_optimization_candidates (review_count, last_optimized, is_active)
    
) ENGINE=InnoDB COMMENT 'Enhanced FSRS user parameters with complete personalization support';

-- =============================================================================
-- 2. Security Audit and Event Logging Tables
-- =============================================================================

-- Create comprehensive security events table
CREATE TABLE security_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Security event ID',
    
    -- Event Classification
    event_type VARCHAR(50) NOT NULL COMMENT 'Type of security event',
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL COMMENT 'Event severity level',
    category ENUM('AUTHENTICATION', 'AUTHORIZATION', 'INPUT_VALIDATION', 'RATE_LIMIT', 'SUSPICIOUS_ACTIVITY', 'SYSTEM') NOT NULL COMMENT 'Event category',
    
    -- User and Request Context
    user_id BIGINT COMMENT 'User ID if authenticated (logical reference)',
    ip_address VARCHAR(45) NOT NULL COMMENT 'Source IP address (supports IPv4 and IPv6)',
    user_agent TEXT COMMENT 'User agent string',
    request_uri VARCHAR(500) COMMENT 'Request URI that triggered event',
    request_method VARCHAR(10) COMMENT 'HTTP request method',
    
    -- Event Details
    description TEXT NOT NULL COMMENT 'Human-readable event description',
    event_data JSON COMMENT 'Additional event data in JSON format',
    correlation_id VARCHAR(36) COMMENT 'Correlation ID for request tracing',
    session_id VARCHAR(100) COMMENT 'Session ID if available',
    
    -- Geographic and Network Context
    country_code CHAR(2) COMMENT 'Country code from IP geolocation',
    city VARCHAR(100) COMMENT 'City from IP geolocation',
    organization VARCHAR(200) COMMENT 'ISP/Organization from IP lookup',
    
    -- Response and Action
    action_taken VARCHAR(100) COMMENT 'Action taken in response to event',
    blocked BOOLEAN DEFAULT FALSE COMMENT 'Whether request was blocked',
    
    -- Timing
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When event occurred',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When record was created',
    
    -- Indexes for Performance and Analysis
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_category (category),
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_event_timestamp (event_timestamp),
    INDEX idx_correlation_id (correlation_id),
    INDEX idx_blocked (blocked),
    
    -- Composite indexes for security analysis
    INDEX idx_ip_severity_time (ip_address, severity, event_timestamp),
    INDEX idx_user_category_time (user_id, category, event_timestamp),
    INDEX idx_critical_events (severity, event_timestamp) WHERE severity IN ('HIGH', 'CRITICAL'),
    INDEX idx_recent_events (event_timestamp, severity, category),
    INDEX idx_suspicious_activity (category, ip_address, event_timestamp) WHERE category = 'SUSPICIOUS_ACTIVITY'
    
) ENGINE=InnoDB COMMENT 'Security events and audit log';

-- Create user sessions table for enhanced session management
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Session ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference)',
    
    -- Session Identification
    session_token VARCHAR(255) UNIQUE NOT NULL COMMENT 'Session token (hashed)',
    jti VARCHAR(36) UNIQUE NOT NULL COMMENT 'JWT ID for token tracking',
    
    -- Session Context
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP address of session (supports IPv4 and IPv6)',
    user_agent TEXT COMMENT 'User agent string',
    device_fingerprint VARCHAR(100) COMMENT 'Device fingerprint hash',
    
    -- Geographic Context
    country_code CHAR(2) COMMENT 'Country code',
    city VARCHAR(100) COMMENT 'City',
    
    -- Session Lifecycle
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Session creation time',
    expires_at TIMESTAMP NOT NULL COMMENT 'Session expiration time',
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Last access time',
    last_activity VARCHAR(100) COMMENT 'Last activity description',
    
    -- Session Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Session active status',
    invalidated_at TIMESTAMP NULL COMMENT 'When session was invalidated',
    invalidation_reason VARCHAR(100) COMMENT 'Reason for invalidation',
    
    -- Security Flags
    is_suspicious BOOLEAN DEFAULT FALSE COMMENT 'Marked as suspicious activity',
    failed_attempts INTEGER DEFAULT 0 COMMENT 'Failed authentication attempts',
    
    -- Indexes
    INDEX idx_user_id (user_id),
    INDEX idx_session_token (session_token),
    INDEX idx_jti (jti),
    INDEX idx_expires_at (expires_at),
    INDEX idx_ip_address (ip_address),
    INDEX idx_active (is_active),
    INDEX idx_suspicious (is_suspicious),
    
    -- Composite indexes
    INDEX idx_user_active_expires (user_id, is_active, expires_at),
    INDEX idx_cleanup (expires_at, is_active) -- For automated cleanup
    
) ENGINE=InnoDB COMMENT 'User session tracking and management';

-- Create rate limit tracking table
CREATE TABLE rate_limit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Rate limit event ID',
    
    -- Target Information
    target_type ENUM('USER', 'IP', 'COMBINED') NOT NULL COMMENT 'What was rate limited',
    target_id VARCHAR(100) NOT NULL COMMENT 'User ID or IP address',
    user_id BIGINT COMMENT 'User ID if authenticated',
    ip_address VARCHAR(45) NOT NULL COMMENT 'Source IP address (supports IPv4 and IPv6)',
    
    -- Rate Limit Details
    endpoint VARCHAR(200) NOT NULL COMMENT 'Endpoint that was accessed',
    request_count INTEGER NOT NULL COMMENT 'Number of requests in window',
    rate_limit INTEGER NOT NULL COMMENT 'Rate limit threshold',
    window_seconds INTEGER NOT NULL COMMENT 'Rate limit window in seconds',
    
    -- Violation Information
    exceeded_by INTEGER NOT NULL COMMENT 'How much the limit was exceeded by',
    backoff_seconds INTEGER COMMENT 'Backoff period applied',
    violation_count INTEGER DEFAULT 1 COMMENT 'Number of violations for this target',
    
    -- Context
    user_agent TEXT COMMENT 'User agent string',
    request_method VARCHAR(10) NOT NULL COMMENT 'HTTP method',
    
    -- Timestamp
    event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When violation occurred',
    
    -- Indexes
    INDEX idx_target (target_type, target_id),
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_endpoint (endpoint),
    INDEX idx_event_timestamp (event_timestamp),
    INDEX idx_violation_count (violation_count),
    
    -- Composite indexes for analysis
    INDEX idx_target_violations (target_id, violation_count, event_timestamp),
    INDEX idx_recent_violations (event_timestamp, target_type) WHERE event_timestamp > DATE_SUB(NOW(), INTERVAL 1 HOUR)
    
) ENGINE=InnoDB COMMENT 'Rate limiting events and violations';

-- =============================================================================
-- 3. Enhanced FSRS Cards Table Updates
-- =============================================================================

-- Add missing indexes and constraints to fsrs_cards for better performance
ALTER TABLE fsrs_cards 
    ADD COLUMN elapsed_days INTEGER COMMENT 'Days elapsed since last review',
    ADD COLUMN scheduled_days INTEGER COMMENT 'Scheduled interval in days',
    ADD COLUMN reps INTEGER DEFAULT 0 COMMENT 'Number of repetitions',
    ADD COLUMN grade INTEGER COMMENT 'Last review grade/rating',
    
    -- Add performance indexes for large-scale operations
    ADD INDEX idx_user_state_due (user_id, state, due_date),
    ADD INDEX idx_overdue_priority (due_date, user_id) WHERE due_date < CURDATE(),
    ADD INDEX idx_stability_range (stability, user_id),
    ADD INDEX idx_difficulty_range (difficulty, user_id),
    
    -- Add composite index for queue generation optimization
    ADD INDEX idx_queue_generation (user_id, state, due_date, next_review_at, difficulty);

-- =============================================================================
-- 4. Enhanced Review Logs for Parameter Optimization
-- =============================================================================

-- Add columns needed for advanced FSRS parameter optimization
ALTER TABLE review_logs
    ADD COLUMN elapsed_days INTEGER COMMENT 'Days since last review (for optimization)',
    ADD COLUMN last_elapsed_days INTEGER COMMENT 'Previous elapsed days (for optimization)',
    ADD COLUMN scheduled_days INTEGER COMMENT 'Originally scheduled days',
    ADD COLUMN reps INTEGER COMMENT 'Number of reps at time of review',
    ADD COLUMN lapses INTEGER COMMENT 'Number of lapses at time of review',
    
    -- Add optimization-specific indexes
    ADD INDEX idx_optimization_data (user_id, reviewed_at, rating, elapsed_days, last_elapsed_days),
    ADD INDEX idx_parameter_training (user_id, reviewed_at) WHERE reviewed_at > DATE_SUB(NOW(), INTERVAL 1 YEAR);

-- =============================================================================
-- 5. Performance Monitoring and Metrics Tables
-- =============================================================================

-- Create performance metrics table
CREATE TABLE performance_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Metric ID',
    
    -- Metric Classification
    metric_name VARCHAR(100) NOT NULL COMMENT 'Name of the metric',
    metric_category ENUM('FSRS', 'AUTHENTICATION', 'DATABASE', 'API', 'SECURITY') NOT NULL COMMENT 'Metric category',
    
    -- Metric Values
    value_double DOUBLE COMMENT 'Numeric value for the metric',
    value_integer BIGINT COMMENT 'Integer value for the metric',
    value_text VARCHAR(500) COMMENT 'Text value for the metric',
    
    -- Context
    user_id BIGINT COMMENT 'User ID if user-specific metric',
    additional_context JSON COMMENT 'Additional context in JSON',
    
    -- Timing
    measurement_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When metric was measured',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When record was created',
    
    -- Indexes
    INDEX idx_metric_name (metric_name),
    INDEX idx_category (metric_category),
    INDEX idx_measurement_time (measurement_timestamp),
    INDEX idx_user_metric (user_id, metric_name),
    
    -- Composite indexes for analysis
    INDEX idx_category_time_value (metric_category, measurement_timestamp, value_double),
    INDEX idx_recent_metrics (measurement_timestamp, metric_name) WHERE measurement_timestamp > DATE_SUB(NOW(), INTERVAL 24 HOUR)
    
) ENGINE=InnoDB COMMENT 'Performance metrics and monitoring data';

-- =============================================================================
-- 6. Data Migration and Initial Setup
-- =============================================================================

-- Migrate existing user_parameters data if it exists
INSERT INTO user_fsrs_parameters (
    user_id, is_active, is_optimized, version, created_at
)
SELECT DISTINCT 
    user_id, 
    TRUE, 
    FALSE, 
    'FSRS-4.5', 
    NOW()
FROM users 
WHERE deleted = 0 AND is_active = TRUE
AND NOT EXISTS (
    SELECT 1 FROM user_fsrs_parameters ufp WHERE ufp.user_id = users.id
);

-- =============================================================================
-- 7. Create Optimized Views for Common Queries
-- =============================================================================

-- View for user FSRS parameters with optimization status
CREATE VIEW user_fsrs_status AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    ufp.is_optimized,
    ufp.review_count,
    ufp.performance_improvement,
    ufp.optimization_accuracy,
    ufp.last_optimized,
    CASE 
        WHEN ufp.review_count >= 1000 THEN 'READY_FOR_OPTIMIZATION'
        WHEN ufp.review_count >= 100 THEN 'PARTIAL_DATA'
        ELSE 'INSUFFICIENT_DATA'
    END as optimization_status,
    DATEDIFF(NOW(), ufp.last_optimized) as days_since_optimization
FROM users u
LEFT JOIN user_fsrs_parameters ufp ON u.id = ufp.user_id AND ufp.is_active = TRUE
WHERE u.deleted = 0 AND u.is_active = TRUE;

-- View for security monitoring dashboard
CREATE VIEW security_dashboard AS
SELECT 
    DATE(event_timestamp) as event_date,
    category,
    severity,
    COUNT(*) as event_count,
    COUNT(DISTINCT ip_address) as unique_ips,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(CASE WHEN blocked = TRUE THEN 1 END) as blocked_count
FROM security_events 
WHERE event_timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(event_timestamp), category, severity
ORDER BY event_date DESC, severity DESC;

-- View for rate limit monitoring
CREATE VIEW rate_limit_summary AS
SELECT 
    DATE(event_timestamp) as event_date,
    target_type,
    endpoint,
    COUNT(*) as violation_count,
    COUNT(DISTINCT target_id) as unique_targets,
    AVG(exceeded_by) as avg_exceeded_by,
    MAX(violation_count) as max_violations_per_target
FROM rate_limit_events
WHERE event_timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
GROUP BY DATE(event_timestamp), target_type, endpoint
ORDER BY event_date DESC, violation_count DESC;

-- =============================================================================
-- 8. Stored Procedures for Maintenance
-- =============================================================================

-- Procedure to clean up expired sessions
DELIMITER $$
CREATE PROCEDURE CleanupExpiredSessions()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Mark expired sessions as inactive
    UPDATE user_sessions 
    SET is_active = FALSE, 
        invalidated_at = NOW(),
        invalidation_reason = 'EXPIRED'
    WHERE expires_at < NOW() 
      AND is_active = TRUE;
    
    -- Delete sessions older than 30 days
    DELETE FROM user_sessions 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
      AND is_active = FALSE;
    
    COMMIT;
END$$
DELIMITER ;

-- Procedure to clean up old security events
DELIMITER $$
CREATE PROCEDURE CleanupSecurityEvents()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Keep HIGH and CRITICAL events for 1 year
    DELETE FROM security_events 
    WHERE event_timestamp < DATE_SUB(NOW(), INTERVAL 1 YEAR)
      AND severity NOT IN ('HIGH', 'CRITICAL');
    
    -- Keep HIGH and CRITICAL events for 2 years
    DELETE FROM security_events 
    WHERE event_timestamp < DATE_SUB(NOW(), INTERVAL 2 YEAR)
      AND severity IN ('HIGH', 'CRITICAL');
    
    COMMIT;
END$$
DELIMITER ;

-- =============================================================================
-- 9. Create Events for Automated Maintenance
-- =============================================================================

-- Event to cleanup expired sessions daily
CREATE EVENT IF NOT EXISTS cleanup_expired_sessions
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '02:00:00')
DO
  CALL CleanupExpiredSessions();

-- Event to cleanup old security events weekly
CREATE EVENT IF NOT EXISTS cleanup_security_events
ON SCHEDULE EVERY 1 WEEK
STARTS TIMESTAMP(CURRENT_DATE + INTERVAL 1 DAY, '03:00:00')
DO
  CALL CleanupSecurityEvents();

-- =============================================================================
-- 10. Final Optimizations
-- =============================================================================

-- Analyze tables for better query optimization
ANALYZE TABLE user_fsrs_parameters;
ANALYZE TABLE security_events;
ANALYZE TABLE user_sessions;
ANALYZE TABLE rate_limit_events;
ANALYZE TABLE performance_metrics;
ANALYZE TABLE fsrs_cards;
ANALYZE TABLE review_logs;

-- Update table statistics
UPDATE INFORMATION_SCHEMA.TABLES SET TABLE_COMMENT = CONCAT(TABLE_COMMENT, ' - Enhanced v2.0.0') 
WHERE TABLE_SCHEMA = 'codetop_fsrs' 
  AND TABLE_NAME IN ('user_fsrs_parameters', 'fsrs_cards', 'review_logs');

COMMIT;