-- CodeTop FSRS Backend Database Schema
-- Using logical foreign keys only (no physical foreign key constraints)
-- Compatible with MyBatis-Plus requirements

-- Note: Database creation and selection is handled by the application
-- CREATE DATABASE and USE statements are removed for compatibility with TestContainers

-- ===================================
-- 1. Users Table
-- ===================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Unique username',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Email address',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Encrypted password',
    
    -- Profile Information
    first_name VARCHAR(50) COMMENT 'First name',
    last_name VARCHAR(50) COMMENT 'Last name',
    avatar_url VARCHAR(500) COMMENT 'Avatar image URL',
    timezone VARCHAR(50) DEFAULT 'UTC' COMMENT 'User timezone',
    
    -- OAuth Information
    oauth_provider VARCHAR(20) COMMENT 'OAuth provider (github, google)',
    oauth_id VARCHAR(100) COMMENT 'OAuth provider user ID',
    
    -- Status and Preferences
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Account active status',
    is_email_verified BOOLEAN DEFAULT FALSE COMMENT 'Email verification status',
    preferences JSON COMMENT 'User preferences in JSON format',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    last_login_at TIMESTAMP NULL COMMENT 'Last login time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag (0=active, 1=deleted)',
    
    -- Indexes
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_oauth (oauth_provider, oauth_id),
    INDEX idx_last_login (last_login_at),
    INDEX idx_active (is_active, deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT 'User accounts table';

-- ===================================
-- 2. Companies Table
-- ===================================
CREATE TABLE companies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Company ID',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Company name',
    display_name VARCHAR(100) COMMENT 'Display name for UI',
    description TEXT COMMENT 'Company description',
    website_url VARCHAR(200) COMMENT 'Company website',
    logo_url VARCHAR(500) COMMENT 'Company logo URL',
    location VARCHAR(100) COMMENT 'Company location',
    industry VARCHAR(50) COMMENT 'Company industry',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Company active status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    INDEX idx_name (name),
    INDEX idx_active (is_active, deleted),
    INDEX idx_industry (industry)
) ENGINE=InnoDB COMMENT 'Companies table for problem associations';

-- ===================================
-- 3. Categories Table
-- ===================================
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Category ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Category name',
    display_name VARCHAR(100) COMMENT 'Display name for UI',
    description TEXT COMMENT 'Category description',
    color_code VARCHAR(7) COMMENT 'Color code for UI display (#RRGGBB)',
    
    -- Hierarchy (for future use)
    parent_category_id BIGINT COMMENT 'Parent category ID (logical reference)',
    level TINYINT DEFAULT 1 COMMENT 'Category level in hierarchy',
    sort_order INT DEFAULT 0 COMMENT 'Sort order for display',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Category active status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    INDEX idx_name (name),
    INDEX idx_parent (parent_category_id),
    INDEX idx_level (level),
    INDEX idx_active (is_active, deleted),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB COMMENT 'Problem categories table';

-- ===================================
-- 4. Problems Table
-- ===================================
CREATE TABLE problems (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Problem ID',
    title VARCHAR(200) NOT NULL COMMENT 'Problem title',
    description TEXT COMMENT 'Problem description',
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL COMMENT 'Problem difficulty level',
    
    -- Problem Details
    problem_url VARCHAR(500) COMMENT 'Original problem URL',
    solution_template TEXT COMMENT 'Solution template code',
    hints JSON COMMENT 'Hints array in JSON format',
    constraints JSON COMMENT 'Constraints array in JSON format',
    examples JSON COMMENT 'Examples array in JSON format',
    
    -- Statistics
    acceptance_rate DECIMAL(5,2) COMMENT 'Acceptance rate percentage',
    solution_count INT DEFAULT 0 COMMENT 'Number of solutions',
    like_count INT DEFAULT 0 COMMENT 'Number of likes',
    dislike_count INT DEFAULT 0 COMMENT 'Number of dislikes',
    
    -- Metadata
    tags JSON COMMENT 'Problem tags array in JSON format',
    topics JSON COMMENT 'Related topics array in JSON format',
    
    -- Status
    is_premium BOOLEAN DEFAULT FALSE COMMENT 'Premium problem flag',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Problem active status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    INDEX idx_difficulty (difficulty),
    INDEX idx_active (is_active, deleted),
    INDEX idx_premium (is_premium),
    INDEX idx_acceptance_rate (acceptance_rate),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_title_description (title, description)
) ENGINE=InnoDB COMMENT 'Algorithm problems table';

-- ===================================
-- 5. Problem Companies Association Table
-- ===================================
CREATE TABLE problem_companies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Association ID',
    problem_id BIGINT NOT NULL COMMENT 'Problem ID (logical reference to problems.id)',
    company_id BIGINT NOT NULL COMMENT 'Company ID (logical reference to companies.id)',
    
    -- Association Details
    frequency ENUM('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH') DEFAULT 'MEDIUM' COMMENT 'Question frequency',
    last_asked_date DATE COMMENT 'Last time this problem was asked',
    times_asked INT DEFAULT 1 COMMENT 'Number of times asked',
    difficulty_level ENUM('EASY', 'MEDIUM', 'HARD') COMMENT 'Company-specific difficulty rating',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Constraints
    UNIQUE KEY uk_problem_company (problem_id, company_id),
    
    -- Indexes
    INDEX idx_problem_id (problem_id),
    INDEX idx_company_id (company_id),
    INDEX idx_frequency (frequency),
    INDEX idx_last_asked (last_asked_date),
    INDEX idx_times_asked (times_asked)
) ENGINE=InnoDB COMMENT 'Problem-Company association table';

-- ===================================
-- 6. FSRS Cards Table
-- ===================================
CREATE TABLE fsrs_cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'FSRS Card ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    problem_id BIGINT NOT NULL COMMENT 'Problem ID (logical reference to problems.id)',
    
    -- FSRS Algorithm State
    state ENUM('NEW', 'LEARNING', 'REVIEW', 'RELEARNING') DEFAULT 'NEW' COMMENT 'Current FSRS state',
    difficulty DECIMAL(10,4) DEFAULT 0.0000 COMMENT 'Current difficulty value',
    stability DECIMAL(10,4) DEFAULT 0.0000 COMMENT 'Current stability value',
    
    -- Review Tracking
    review_count INT DEFAULT 0 COMMENT 'Total number of reviews',
    lapses INT DEFAULT 0 COMMENT 'Number of lapses (failures)',
    
    -- Scheduling
    last_review_at TIMESTAMP NULL COMMENT 'Last review timestamp',
    next_review_at TIMESTAMP NULL COMMENT 'Next scheduled review timestamp',
    due_date DATE GENERATED ALWAYS AS (DATE(next_review_at)) STORED COMMENT 'Due date for indexing',
    interval_days INT DEFAULT 0 COMMENT 'Current interval in days',
    
    -- Legacy Anki Compatibility
    ease_factor DECIMAL(5,4) DEFAULT 2.5000 COMMENT 'Ease factor for Anki compatibility',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Card creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Constraints
    UNIQUE KEY uk_user_problem (user_id, problem_id),
    
    -- Indexes for Performance
    INDEX idx_user_state (user_id, state),
    INDEX idx_user_due (user_id, due_date),
    INDEX idx_next_review (next_review_at),
    INDEX idx_due_date (due_date),
    INDEX idx_state (state),
    INDEX idx_user_id (user_id),
    INDEX idx_problem_id (problem_id),
    
    -- Composite indexes for queue generation
    INDEX idx_queue_priority (user_id, due_date, next_review_at),
    INDEX idx_overdue (user_id, due_date, state) 
) ENGINE=InnoDB COMMENT 'FSRS cards for spaced repetition scheduling';

-- ===================================
-- 7. Review Logs Table
-- ===================================
CREATE TABLE review_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Review log ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    problem_id BIGINT NOT NULL COMMENT 'Problem ID (logical reference to problems.id)',
    card_id BIGINT NOT NULL COMMENT 'FSRS Card ID (logical reference to fsrs_cards.id)',
    session_id BIGINT COMMENT 'Review session ID (logical reference to review_sessions.id)',
    
    -- Review Details
    rating TINYINT NOT NULL COMMENT 'User rating (1=Again, 2=Hard, 3=Good, 4=Easy)',
    response_time_ms INT COMMENT 'Response time in milliseconds',
    
    -- State Tracking
    old_state ENUM('NEW', 'LEARNING', 'REVIEW', 'RELEARNING') COMMENT 'Previous FSRS state',
    new_state ENUM('NEW', 'LEARNING', 'REVIEW', 'RELEARNING') COMMENT 'New FSRS state after review',
    
    -- Algorithm Changes
    stability_before DECIMAL(10,4) COMMENT 'Stability before review',
    stability_after DECIMAL(10,4) COMMENT 'Stability after review',
    difficulty_before DECIMAL(10,4) COMMENT 'Difficulty before review',
    difficulty_after DECIMAL(10,4) COMMENT 'Difficulty after review',
    
    -- Scheduling
    interval_before_days INT COMMENT 'Interval before review (days)',
    interval_after_days INT COMMENT 'Interval after review (days)',
    
    -- Review Type
    review_type ENUM('SCHEDULED', 'EXTRA', 'CRAM') DEFAULT 'SCHEDULED' COMMENT 'Type of review',
    
    -- Timestamps
    reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Review timestamp',
    
    -- Indexes for Analytics
    INDEX idx_user_reviewed_at (user_id, reviewed_at),
    INDEX idx_card_reviewed_at (card_id, reviewed_at),
    INDEX idx_session_id (session_id),
    INDEX idx_rating (rating),
    INDEX idx_user_rating (user_id, rating),
    INDEX idx_problem_id (problem_id),
    INDEX idx_review_type (review_type),
    
    -- Composite indexes for analytics
    INDEX idx_user_date_rating (user_id, reviewed_at, rating),
    INDEX idx_analytics_range (user_id, reviewed_at, rating, response_time_ms)
) ENGINE=InnoDB COMMENT 'Review history logs for analytics and parameter optimization';

-- ===================================
-- 8. Review Sessions Table
-- ===================================
CREATE TABLE review_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Session ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    
    -- Session Details
    session_type ENUM('DAILY_REVIEW', 'EXTRA_PRACTICE', 'CRAM_SESSION', 'EXAM_PREP') DEFAULT 'DAILY_REVIEW' COMMENT 'Type of review session',
    target_count INT DEFAULT 0 COMMENT 'Target number of problems to review',
    
    -- Session Progress
    problems_completed INT DEFAULT 0 COMMENT 'Number of problems completed',
    problems_correct INT DEFAULT 0 COMMENT 'Number of problems answered correctly',
    
    -- Timing
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Session start time',
    ended_at TIMESTAMP NULL COMMENT 'Session end time',
    total_time_ms INT COMMENT 'Total session time in milliseconds',
    
    -- Statistics
    average_response_time_ms INT COMMENT 'Average response time',
    accuracy_rate DECIMAL(5,2) COMMENT 'Session accuracy rate percentage',
    
    -- Status
    status ENUM('IN_PROGRESS', 'COMPLETED', 'INTERRUPTED') DEFAULT 'IN_PROGRESS' COMMENT 'Session status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Indexes
    INDEX idx_user_started (user_id, started_at),
    INDEX idx_user_status (user_id, status),
    INDEX idx_session_type (session_type),
    INDEX idx_ended_at (ended_at),
    INDEX idx_date_range (started_at, ended_at)
) ENGINE=InnoDB COMMENT 'User review sessions for tracking study periods';

-- ===================================
-- 9. User Parameters Table
-- ===================================
CREATE TABLE user_parameters (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Parameter set ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    
    -- FSRS Parameters (17 parameters stored as JSON array)
    parameters JSON NOT NULL COMMENT 'FSRS parameters array (17 values)',
    
    -- Optimization Details
    training_count INT DEFAULT 0 COMMENT 'Number of reviews used for training',
    optimization_loss DECIMAL(10,6) COMMENT 'Optimization loss value',
    r_squared DECIMAL(5,4) COMMENT 'R-squared value for model fit',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether these parameters are currently active',
    is_optimized BOOLEAN DEFAULT FALSE COMMENT 'Whether parameters are user-optimized or default',
    
    -- Timestamps
    optimized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When parameters were optimized',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    
    -- Indexes
    INDEX idx_user_active (user_id, is_active),
    INDEX idx_optimized_at (optimized_at),
    INDEX idx_is_optimized (is_optimized)
) ENGINE=InnoDB COMMENT 'User-specific FSRS parameters for personalized scheduling';

-- ===================================
-- 10. User Statistics Table
-- ===================================
CREATE TABLE user_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Statistics ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT 'User ID (logical reference to users.id)',
    
    -- Problem Statistics
    total_problems INT DEFAULT 0 COMMENT 'Total problems attempted',
    problems_mastered INT DEFAULT 0 COMMENT 'Problems in REVIEW state with high stability',
    problems_learning INT DEFAULT 0 COMMENT 'Problems in LEARNING state',
    problems_new INT DEFAULT 0 COMMENT 'Problems in NEW state',
    problems_relearning INT DEFAULT 0 COMMENT 'Problems in RELEARNING state',
    
    -- Review Statistics
    total_reviews INT DEFAULT 0 COMMENT 'Total number of reviews completed',
    correct_reviews INT DEFAULT 0 COMMENT 'Number of correct reviews (rating >= 3)',
    total_study_time_ms BIGINT DEFAULT 0 COMMENT 'Total study time in milliseconds',
    
    -- Streak Information
    current_streak_days INT DEFAULT 0 COMMENT 'Current consecutive review days',
    longest_streak_days INT DEFAULT 0 COMMENT 'Longest consecutive review streak',
    last_review_date DATE COMMENT 'Last review date for streak calculation',
    
    -- Performance Metrics
    overall_accuracy_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Overall accuracy rate percentage',
    average_response_time_ms INT DEFAULT 0 COMMENT 'Average response time across all reviews',
    retention_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Retention rate based on FSRS predictions',
    
    -- Preferences
    daily_review_target INT DEFAULT 50 COMMENT 'Daily review target count',
    preferred_review_time TIME COMMENT 'Preferred review time',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Indexes
    INDEX idx_user_id (user_id),
    INDEX idx_current_streak (current_streak_days),
    INDEX idx_longest_streak (longest_streak_days),
    INDEX idx_accuracy (overall_accuracy_rate),
    INDEX idx_last_review (last_review_date)
) ENGINE=InnoDB COMMENT 'Aggregated user statistics for dashboard and analytics';

-- ===================================
-- 11. Leaderboard Table
-- ===================================
CREATE TABLE leaderboard_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Leaderboard entry ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    
    -- Ranking Details
    metric_type ENUM('PROBLEMS_SOLVED', 'STREAK_DAYS', 'ACCURACY_RATE', 'STUDY_TIME', 'RETENTION_RATE') NOT NULL COMMENT 'Metric being ranked',
    period_type ENUM('DAILY', 'WEEKLY', 'MONTHLY', 'ALL_TIME') NOT NULL COMMENT 'Ranking period',
    period_start DATE NOT NULL COMMENT 'Period start date',
    period_end DATE NOT NULL COMMENT 'Period end date',
    
    -- Score and Ranking
    score DECIMAL(10,2) NOT NULL COMMENT 'Score for this metric and period',
    rank_position INT NOT NULL COMMENT 'Rank position (1 = highest)',
    percentile DECIMAL(5,2) COMMENT 'Percentile ranking',
    
    -- Badge Information
    badge_level ENUM('BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND') COMMENT 'Achievement badge level',
    
    -- Timestamps
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'When ranking was calculated',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    
    -- Constraints
    UNIQUE KEY uk_user_metric_period (user_id, metric_type, period_type, period_start),
    
    -- Indexes
    INDEX idx_metric_period_rank (metric_type, period_type, rank_position),
    INDEX idx_user_rankings (user_id, metric_type, period_type),
    INDEX idx_period_range (period_start, period_end),
    INDEX idx_badge_level (badge_level),
    INDEX idx_calculated_at (calculated_at)
) ENGINE=InnoDB COMMENT 'Leaderboard rankings for different metrics and time periods';

-- ===================================
-- 12. Problem Notes Table
-- ===================================
CREATE TABLE problem_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Note ID',
    user_id BIGINT NOT NULL COMMENT 'User ID (logical reference to users.id)',
    problem_id BIGINT NOT NULL COMMENT 'Problem ID (logical reference to problems.id)',
    
    -- Note Content
    title VARCHAR(200) COMMENT 'Note title',
    content TEXT COMMENT 'Note content in markdown format',
    tags JSON COMMENT 'Note tags for organization',
    
    -- Note Type
    note_type ENUM('SOLUTION', 'APPROACH', 'MISTAKE', 'INSIGHT', 'REFERENCE') DEFAULT 'SOLUTION' COMMENT 'Type of note',
    is_public BOOLEAN DEFAULT FALSE COMMENT 'Whether note is publicly visible',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Note creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    INDEX idx_user_problem (user_id, problem_id),
    INDEX idx_user_notes (user_id, created_at),
    INDEX idx_problem_notes (problem_id, is_public),
    INDEX idx_note_type (note_type),
    INDEX idx_public_notes (is_public, deleted),
    FULLTEXT idx_content_search (title, content)
) ENGINE=InnoDB COMMENT 'User notes and solutions for problems';

-- ===================================
-- Create views for common queries
-- ===================================

-- View for user dashboard data
CREATE VIEW user_dashboard_view AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    us.total_problems,
    us.problems_mastered,
    us.current_streak_days,
    us.overall_accuracy_rate,
    us.total_study_time_ms,
    us.last_review_date,
    COUNT(CASE WHEN fc.due_date <= CURDATE() THEN 1 END) as due_today,
    COUNT(CASE WHEN fc.due_date < CURDATE() THEN 1 END) as overdue_count
FROM users u
LEFT JOIN user_statistics us ON u.id = us.user_id
LEFT JOIN fsrs_cards fc ON u.id = fc.user_id AND fc.state IN ('LEARNING', 'REVIEW', 'RELEARNING')
WHERE u.deleted = 0 AND u.is_active = 1
GROUP BY u.id, u.username, u.email, us.total_problems, us.problems_mastered, 
         us.current_streak_days, us.overall_accuracy_rate, us.total_study_time_ms, us.last_review_date;

-- View for problem statistics
CREATE VIEW problem_stats_view AS
SELECT 
    p.id as problem_id,
    p.title,
    p.difficulty,
    COUNT(DISTINCT fc.user_id) as attempted_by_users,
    COUNT(CASE WHEN fc.state = 'REVIEW' AND fc.stability > 7 THEN 1 END) as mastered_by_users,
    AVG(CASE WHEN rl.rating >= 3 THEN 100.0 ELSE 0.0 END) as success_rate,
    AVG(rl.response_time_ms) as avg_response_time_ms
FROM problems p
LEFT JOIN fsrs_cards fc ON p.id = fc.problem_id
LEFT JOIN review_logs rl ON p.id = rl.problem_id
WHERE p.deleted = 0 AND p.is_active = 1
GROUP BY p.id, p.title, p.difficulty;

-- ===================================
-- Initial Data Inserts
-- ===================================

-- Insert sample categories
INSERT INTO categories (name, display_name, description, color_code) VALUES
('array', 'Array', 'Array manipulation and algorithms', '#FF5722'),
('string', 'String', 'String processing and manipulation', '#2196F3'),
('tree', 'Binary Tree', 'Binary tree algorithms and traversal', '#4CAF50'),
('graph', 'Graph', 'Graph algorithms and traversal', '#9C27B0'),
('dp', 'Dynamic Programming', 'Dynamic programming problems', '#FF9800'),
('greedy', 'Greedy', 'Greedy algorithm problems', '#607D8B'),
('backtrack', 'Backtracking', 'Backtracking algorithm problems', '#795548'),
('sort', 'Sorting', 'Sorting algorithms and applications', '#E91E63'),
('search', 'Search', 'Binary search and variations', '#3F51B5'),
('math', 'Math', 'Mathematical problems and algorithms', '#009688');

-- Insert sample companies
INSERT INTO companies (name, display_name, description, website_url, industry) VALUES
('google', 'Google', 'Technology giant focusing on search and cloud', 'https://www.google.com', 'Technology'),
('apple', 'Apple', 'Consumer electronics and software company', 'https://www.apple.com', 'Technology'),
('microsoft', 'Microsoft', 'Software and cloud computing company', 'https://www.microsoft.com', 'Technology'),
('amazon', 'Amazon', 'E-commerce and cloud computing company', 'https://www.amazon.com', 'Technology'),
('facebook', 'Meta (Facebook)', 'Social media and virtual reality company', 'https://www.meta.com', 'Technology'),
('netflix', 'Netflix', 'Streaming entertainment company', 'https://www.netflix.com', 'Entertainment'),
('uber', 'Uber', 'Ride-sharing and delivery company', 'https://www.uber.com', 'Transportation'),
('airbnb', 'Airbnb', 'Online marketplace for lodging', 'https://www.airbnb.com', 'Hospitality'),
('linkedin', 'LinkedIn', 'Professional networking platform', 'https://www.linkedin.com', 'Technology'),
('twitter', 'X (Twitter)', 'Social media platform', 'https://x.com', 'Technology');

-- ===================================
-- Optimize table storage
-- ===================================

-- Optimize tables for better performance
ALTER TABLE review_logs ENGINE=InnoDB ROW_FORMAT=COMPRESSED;
ALTER TABLE fsrs_cards ENGINE=InnoDB ROW_FORMAT=COMPRESSED;