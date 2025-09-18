-- V10__Remove_Incorrect_Accuracy_Fields.sql
-- Remove accuracy fields that were incorrectly calculated based on rating >= 3
-- These fields were misleading as they represented user difficulty ratings, not actual accuracy

-- Remove accuracy_rate from review_sessions table
-- This field was calculated based on rating >= 3 which represents difficulty, not correctness
ALTER TABLE review_sessions DROP COLUMN IF EXISTS accuracy_rate;

-- Remove overall_accuracy_rate from user_statistics table  
-- This field was also based on the incorrect rating >= 3 calculation
ALTER TABLE user_statistics DROP COLUMN IF EXISTS overall_accuracy_rate;

-- Remove correct_reviews from user_statistics table
-- This field was calculated as count of rating >= 3, which represents difficulty not correctness
ALTER TABLE user_statistics DROP COLUMN IF EXISTS correct_reviews;

-- Remove the accuracy index from user_statistics table
ALTER TABLE user_statistics DROP INDEX IF EXISTS idx_accuracy;

-- Add comment to clarify what we're keeping
ALTER TABLE user_statistics MODIFY COLUMN retention_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT 'FSRS-based retention rate (correct metric)';

-- Note: We keep optimization_accuracy in user_fsrs_parameters as it represents 
-- the actual prediction accuracy of the FSRS algorithm, which is a valid metric