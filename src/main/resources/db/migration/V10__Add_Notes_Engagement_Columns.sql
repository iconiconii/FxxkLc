-- Migration: Add engagement columns to problem_notes table
-- Date: 2025-08-27
-- Description: Add helpful_votes and view_count columns to support note engagement features

-- Add helpful_votes column for tracking note usefulness
ALTER TABLE problem_notes 
ADD COLUMN helpful_votes INT DEFAULT 0 COMMENT 'Number of helpful votes received';

-- Add view_count column for tracking note views
ALTER TABLE problem_notes 
ADD COLUMN view_count INT DEFAULT 0 COMMENT 'Number of times note has been viewed';

-- Add indexes for performance on sorting by engagement metrics
CREATE INDEX idx_notes_helpful_votes ON problem_notes (helpful_votes DESC, updated_at DESC);
CREATE INDEX idx_notes_view_count ON problem_notes (view_count DESC, updated_at DESC);
CREATE INDEX idx_notes_engagement ON problem_notes (is_public, deleted, helpful_votes DESC, view_count DESC);

-- Update existing rows to have default values
UPDATE problem_notes SET helpful_votes = 0 WHERE helpful_votes IS NULL;
UPDATE problem_notes SET view_count = 0 WHERE view_count IS NULL;