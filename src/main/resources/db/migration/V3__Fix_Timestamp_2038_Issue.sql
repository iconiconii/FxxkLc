-- Fix Year 2038 Problem for FSRS Cards
-- Change TIMESTAMP to DATETIME for date fields that can go beyond 2038

-- Modify fsrs_cards table to use DATETIME instead of TIMESTAMP for next_review_at and last_review_at
ALTER TABLE fsrs_cards 
  MODIFY COLUMN last_review_at DATETIME(6) NULL,
  MODIFY COLUMN next_review_at DATETIME(6) NULL;

-- Update the generated column for due_date to work with DATETIME
ALTER TABLE fsrs_cards 
  DROP COLUMN due_date,
  ADD COLUMN due_date DATE AS (DATE(next_review_at)) STORED;

-- Recreate the index on next_review_at
DROP INDEX IF EXISTS idx_fsrs_cards_next_review_at ON fsrs_cards;
CREATE INDEX idx_fsrs_cards_next_review_at ON fsrs_cards(next_review_at);

-- Recreate the index on due_date  
DROP INDEX IF EXISTS idx_fsrs_cards_due_date ON fsrs_cards;
CREATE INDEX idx_fsrs_cards_due_date ON fsrs_cards(due_date);

-- Also fix review_logs table if it has similar issues
ALTER TABLE review_logs 
  MODIFY COLUMN reviewed_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Comment: This migration fixes the Year 2038 problem by using DATETIME(6) 
-- which supports dates up to 9999-12-31, solving FSRS algorithm overflow issues