-- H2 Database Schema for CodeTop FSRS Backend Testing

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(100),
    avatar_url VARCHAR(255),
    auth_provider VARCHAR(20) DEFAULT 'LOCAL',
    provider_id VARCHAR(100),
    email_verified BOOLEAN DEFAULT FALSE,
    account_locked BOOLEAN DEFAULT FALSE,
    failed_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- Problems table
CREATE TABLE problems (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    title_en VARCHAR(255),
    difficulty VARCHAR(20) NOT NULL,
    frequency INT DEFAULT 0,
    acceptance_rate DECIMAL(5,2),
    tags TEXT,
    companies TEXT,
    url VARCHAR(500),
    description TEXT,
    leetcode_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- FSRS Cards table
CREATE TABLE fsrs_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id INT NOT NULL,
    difficulty DECIMAL(10,6) DEFAULT 5.0,
    stability DECIMAL(10,6) DEFAULT 1.0,
    state INT DEFAULT 0,
    review_count INT DEFAULT 0,
    lapses INT DEFAULT 0,
    last_review TIMESTAMP,
    next_review TIMESTAMP NOT NULL,
    elapsed_days INT DEFAULT 0,
    scheduled_days INT DEFAULT 1,
    reps INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(user_id, problem_id)
);

-- Review Logs table
CREATE TABLE review_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id INT NOT NULL,
    rating INT NOT NULL,
    state INT NOT NULL,
    review_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    elapsed_days INT NOT NULL,
    scheduled_days INT NOT NULL,
    review_duration_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Parameters table (for FSRS personalization)
CREATE TABLE user_parameters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    parameters TEXT, -- JSON array of FSRS parameters
    optimization_date TIMESTAMP,
    review_count INT DEFAULT 0,
    performance_score DECIMAL(5,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Companies table
CREATE TABLE companies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    name_en VARCHAR(100),
    logo_url VARCHAR(255),
    website VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_fsrs_cards_user_next_review ON fsrs_cards(user_id, next_review);
CREATE INDEX idx_fsrs_cards_next_review ON fsrs_cards(next_review);
CREATE INDEX idx_review_logs_card_id ON review_logs(card_id);
CREATE INDEX idx_review_logs_user_id ON review_logs(user_id);
CREATE INDEX idx_review_logs_review_time ON review_logs(review_time);
CREATE INDEX idx_problems_difficulty ON problems(difficulty);
CREATE INDEX idx_problems_frequency ON problems(frequency DESC);