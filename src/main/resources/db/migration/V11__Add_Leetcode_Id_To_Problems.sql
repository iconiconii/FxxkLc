-- Add leetcode_id column to problems table
-- This field stores the LeetCode problem ID for cross-referencing

ALTER TABLE problems 
ADD COLUMN leetcode_id VARCHAR(50) NULL COMMENT 'LeetCode problem identifier';

-- Add index for better query performance
CREATE INDEX idx_problems_leetcode_id ON problems(leetcode_id);