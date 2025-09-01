-- V3: Simplify Problems Table Structure
-- Remove redundant fields that can be accessed via problem_url
-- Keep only essential fields for FSRS algorithm functionality

-- First, create a backup table (optional, for safety)
CREATE TABLE problems_backup AS SELECT * FROM problems;

-- Remove redundant columns from problems table
ALTER TABLE problems 
    DROP COLUMN description,
    DROP COLUMN solution_template,
    DROP COLUMN hints,
    DROP COLUMN constraints,
    DROP COLUMN examples,
    DROP COLUMN acceptance_rate,
    DROP COLUMN solution_count,
    DROP COLUMN like_count,
    DROP COLUMN dislike_count,
    DROP COLUMN topics;

-- Update the full-text search index to only include title
-- First drop the existing index
DROP INDEX idx_title_description ON problems;

-- Create new index only on title
ALTER TABLE problems ADD FULLTEXT INDEX idx_title_search (title);

-- Optimize table structure
OPTIMIZE TABLE problems;

-- Update table comment
ALTER TABLE problems COMMENT = 'Simplified algorithm problems table - content accessed via problem_url';