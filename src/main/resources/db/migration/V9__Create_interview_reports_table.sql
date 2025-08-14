-- Create interview_reports table for community-driven interview data collection
CREATE TABLE interview_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT 'User ID who submitted the report',
    company_name VARCHAR(100) NOT NULL COMMENT 'Company name',
    department VARCHAR(100) NOT NULL COMMENT 'Department name',
    position VARCHAR(100) NOT NULL COMMENT 'Position name',
    problem_title VARCHAR(255) NOT NULL COMMENT 'Problem title',
    problem_leetcode_id VARCHAR(50) COMMENT 'LeetCode problem ID',
    interview_date DATE NOT NULL COMMENT 'Interview date',
    interview_round ENUM('PHONE', 'TECHNICAL', 'ONSITE', 'FINAL', 'OTHER') DEFAULT 'OTHER' COMMENT 'Interview round',
    difficulty_rating INT COMMENT 'Difficulty rating (1-5 scale)',
    additional_notes TEXT COMMENT 'Additional notes about the interview',
    is_verified BOOLEAN DEFAULT FALSE COMMENT 'Whether the report is verified',
    verification_notes TEXT COMMENT 'Verification notes',
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING' COMMENT 'Report status',
    department_id BIGINT COMMENT 'Standardized department ID',
    position_id BIGINT COMMENT 'Standardized position ID',
    
    -- Enhanced data source management fields
    data_source ENUM('USER_REPORT', 'OFFICIAL_COMPANY', 'CROWDSOURCING', 'AUTOMATED_SCRAPING', 'ADMIN_INPUT') DEFAULT 'USER_REPORT' COMMENT 'Source of the interview report data',
    source_url VARCHAR(500) COMMENT 'URL source of the report (if applicable)',
    credibility_score DECIMAL(5,2) DEFAULT 50.00 COMMENT 'Credibility score (0-100) based on source and verification',
    verification_level ENUM('UNVERIFIED', 'COMMUNITY_VERIFIED', 'MODERATOR_VERIFIED', 'OFFICIAL_VERIFIED') DEFAULT 'UNVERIFIED' COMMENT 'Level of verification',
    verified_by_user_id BIGINT COMMENT 'User ID who verified this report',
    verified_at TIMESTAMP NULL COMMENT 'Timestamp when verification was completed',
    upvote_count INT DEFAULT 0 COMMENT 'Number of upvotes from community',
    downvote_count INT DEFAULT 0 COMMENT 'Number of downvotes from community',
    report_quality_score DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Quality score based on completeness and accuracy',
    is_duplicate BOOLEAN DEFAULT FALSE COMMENT 'Whether this report is a duplicate of another report',
    original_report_id BIGINT COMMENT 'Reference to original report if this is a duplicate',
    duplicate_check_status ENUM('PENDING', 'CHECKED', 'CONFIRMED_DUPLICATE', 'CONFIRMED_UNIQUE') DEFAULT 'PENDING' COMMENT 'Status of duplicate checking',
    language_code VARCHAR(10) DEFAULT 'zh-CN' COMMENT 'Language code of the report',
    reporting_region VARCHAR(10) DEFAULT 'CN' COMMENT 'Geographic region where interview took place',
    data_collection_method ENUM('MANUAL_ENTRY', 'FORM_SUBMISSION', 'API_IMPORT', 'BULK_UPLOAD', 'SOCIAL_MEDIA_SCRAPING') DEFAULT 'FORM_SUBMISSION' COMMENT 'Method used to collect this data',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes for better query performance
    INDEX idx_company_name (company_name),
    INDEX idx_department (department),
    INDEX idx_position (position),
    INDEX idx_interview_date (interview_date),
    INDEX idx_status (status),
    INDEX idx_verification_level (verification_level),
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_deleted (deleted),
    
    -- Composite indexes for common query patterns
    INDEX idx_company_status (company_name, status, deleted),
    INDEX idx_department_position (department, position, deleted),
    INDEX idx_verification_status (verification_level, status, deleted)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Interview reports for community-driven data collection';