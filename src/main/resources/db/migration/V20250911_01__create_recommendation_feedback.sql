-- Dev migration: create recommendation_feedback table if not exists
-- Mirrors com.codetop.recommendation.entity.RecommendationFeedback

CREATE TABLE IF NOT EXISTS `recommendation_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `feedback` VARCHAR(32) NOT NULL,
  `note` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_user_created` (`user_id`, `created_at`),
  KEY `idx_feedback_problem_created` (`problem_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

