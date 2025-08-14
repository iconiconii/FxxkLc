-- Test Data Setup for FSRS Backend Testing
-- This script provides comprehensive test data for all testing scenarios

-- =============================================================================
-- USERS TEST DATA
-- =============================================================================

INSERT INTO users (id, email, username, password, role, created_at, updated_at, is_deleted) VALUES
(1000, 'testuser@example.com', 'testuser', '$2a$10$rBV2HDeG/VTmS6Jl.ycF6.UMRiQLZzLiOY1pYF3p4N7YJ8xgcHgSC', 'USER', NOW(), NOW(), false),
(1001, 'admin@example.com', 'admin', '$2a$10$rBV2HDeG/VTmS6Jl.ycF6.UMRiQLZzLiOY1pYF3p4N7YJ8xgcHgSC', 'ADMIN', NOW(), NOW(), false),
(1002, 'performance@example.com', 'perfuser', '$2a$10$rBV2HDeG/VTmS6Jl.ycF6.UMRiQLZzLiOY1pYF3p4N7YJ8xgcHgSC', 'USER', NOW(), NOW(), false),
(1003, 'security@example.com', 'secuser', '$2a$10$rBV2HDeG/VTmS6Jl.ycF6.UMRiQLZzLiOY1pYF3p4N7YJ8xgcHgSC', 'USER', NOW(), NOW(), false);

-- =============================================================================
-- COMPANIES TEST DATA
-- =============================================================================

INSERT INTO companies (id, name, description, website, created_at, updated_at, is_deleted) VALUES
(2000, 'Google', 'Technology company specializing in Internet-related services', 'https://google.com', NOW(), NOW(), false),
(2001, 'Microsoft', 'Multinational technology corporation', 'https://microsoft.com', NOW(), NOW(), false),
(2002, 'Amazon', 'E-commerce and cloud computing company', 'https://amazon.com', NOW(), NOW(), false),
(2003, 'Meta', 'Social media and technology company', 'https://meta.com', NOW(), NOW(), false),
(2004, 'Apple', 'Consumer electronics and software company', 'https://apple.com', NOW(), NOW(), false);

-- =============================================================================
-- PROBLEMS TEST DATA - Comprehensive Algorithm Problems
-- =============================================================================

-- Array and String Problems
INSERT INTO problems (id, title, content, difficulty, tags, created_at, updated_at, is_deleted) VALUES
(3000, 'Two Sum', 'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.', 'EASY', 'array,hash-table', NOW(), NOW(), false),
(3001, 'Reverse String', 'Write a function that reverses a string. The input string is given as an array of characters char[].', 'EASY', 'string,two-pointers', NOW(), NOW(), false),
(3002, 'Valid Palindrome', 'A phrase is a palindrome if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward.', 'EASY', 'string,two-pointers', NOW(), NOW(), false),
(3003, 'Maximum Subarray', 'Given an integer array nums, find the contiguous subarray which has the largest sum and return its sum.', 'MEDIUM', 'array,dynamic-programming', NOW(), NOW(), false),
(3004, 'Merge Intervals', 'Given an array of intervals where intervals[i] = [starti, endi], merge all overlapping intervals.', 'MEDIUM', 'array,sorting', NOW(), NOW(), false),

-- Linked List Problems
(3005, 'Reverse Linked List', 'Given the head of a singly linked list, reverse the list, and return the reversed list.', 'EASY', 'linked-list,recursion', NOW(), NOW(), false),
(3006, 'Merge Two Sorted Lists', 'You are given the heads of two sorted linked lists list1 and list2. Merge the two lists in a sorted list.', 'EASY', 'linked-list,recursion', NOW(), NOW(), false),
(3007, 'Linked List Cycle', 'Given head, the head of a linked list, determine if the linked list has a cycle in it.', 'EASY', 'linked-list,two-pointers', NOW(), NOW(), false),

-- Tree Problems
(3008, 'Binary Tree Inorder Traversal', 'Given the root of a binary tree, return the inorder traversal of its nodes values.', 'EASY', 'tree,stack,recursion', NOW(), NOW(), false),
(3009, 'Maximum Depth of Binary Tree', 'Given the root of a binary tree, return its maximum depth.', 'EASY', 'tree,depth-first-search', NOW(), NOW(), false),
(3010, 'Validate Binary Search Tree', 'Given the root of a binary tree, determine if it is a valid binary search tree.', 'MEDIUM', 'tree,depth-first-search', NOW(), NOW(), false),

-- Dynamic Programming Problems
(3011, 'Climbing Stairs', 'You are climbing a staircase. It takes n steps to reach the top. Each time you can either climb 1 or 2 steps.', 'EASY', 'dynamic-programming', NOW(), NOW(), false),
(3012, 'House Robber', 'You are a professional robber planning to rob houses along a street. Each house has a certain amount of money stashed.', 'MEDIUM', 'dynamic-programming', NOW(), NOW(), false),
(3013, 'Coin Change', 'You are given an integer array coins representing coins of different denominations and an integer amount.', 'MEDIUM', 'dynamic-programming,breadth-first-search', NOW(), NOW(), false),

-- Graph Problems
(3014, 'Number of Islands', 'Given an m x n 2D binary grid grid which represents a map of 1s (land) and 0s (water), return the number of islands.', 'MEDIUM', 'graph,depth-first-search,breadth-first-search', NOW(), NOW(), false),
(3015, 'Course Schedule', 'There are a total of numCourses courses you have to take, labeled from 0 to numCourses - 1.', 'MEDIUM', 'graph,topological-sort,depth-first-search', NOW(), NOW(), false),

-- Hard Problems for Advanced Testing
(3016, 'Median of Two Sorted Arrays', 'Given two sorted arrays nums1 and nums2 of size m and n respectively, return the median of the two sorted arrays.', 'HARD', 'array,binary-search,divide-and-conquer', NOW(), NOW(), false),
(3017, 'Regular Expression Matching', 'Given an input string s and a pattern p, implement regular expression matching with support for . and *.', 'HARD', 'string,dynamic-programming,recursion', NOW(), NOW(), false),
(3018, 'Longest Valid Parentheses', 'Given a string containing just the characters ( and ), find the length of the longest valid parentheses substring.', 'HARD', 'string,dynamic-programming,stack', NOW(), NOW(), false),

-- Performance Testing Problems (Large Scale)
(3019, 'Large Scale Array Processing', 'Process arrays with up to 10^6 elements efficiently using various algorithms.', 'MEDIUM', 'array,algorithm-optimization', NOW(), NOW(), false),
(3020, 'Distributed System Design', 'Design a distributed system that can handle high throughput and maintain consistency.', 'HARD', 'system-design,distributed-systems', NOW(), NOW(), false);

-- =============================================================================
-- PROBLEM-COMPANY RELATIONSHIPS
-- =============================================================================

INSERT INTO problem_companies (problem_id, company_id, frequency, created_at) VALUES
-- Google problems
(3000, 2000, 'HIGH', NOW()),
(3003, 2000, 'HIGH', NOW()),
(3008, 2000, 'MEDIUM', NOW()),
(3014, 2000, 'HIGH', NOW()),
(3016, 2000, 'MEDIUM', NOW()),

-- Microsoft problems  
(3001, 2001, 'MEDIUM', NOW()),
(3005, 2001, 'HIGH', NOW()),
(3010, 2001, 'MEDIUM', NOW()),
(3012, 2001, 'HIGH', NOW()),
(3017, 2001, 'LOW', NOW()),

-- Amazon problems
(3002, 2002, 'HIGH', NOW()),
(3004, 2002, 'HIGH', NOW()),
(3006, 2002, 'MEDIUM', NOW()),
(3011, 2002, 'MEDIUM', NOW()),
(3015, 2002, 'HIGH', NOW()),

-- Meta problems
(3007, 2003, 'MEDIUM', NOW()),
(3009, 2003, 'HIGH', NOW()),
(3013, 2003, 'MEDIUM', NOW()),
(3018, 2003, 'LOW', NOW()),

-- Apple problems
(3019, 2004, 'MEDIUM', NOW()),
(3020, 2004, 'LOW', NOW());

-- =============================================================================
-- FSRS CARDS TEST DATA - Various States and Progressions
-- =============================================================================

-- New cards (just started learning)
INSERT INTO fsrs_cards (id, user_id, problem_id, state, difficulty, stability, review_count, lapses, due_date, last_review, grade, created_at, updated_at) VALUES
(4000, 1000, 3000, 'NEW', 0.00, 0.00, 0, 0, NOW(), NULL, NULL, NOW(), NOW()),
(4001, 1000, 3001, 'NEW', 0.00, 0.00, 0, 0, NOW(), NULL, NULL, NOW(), NOW()),
(4002, 1000, 3002, 'NEW', 0.00, 0.00, 0, 0, NOW(), NULL, NULL, NOW(), NOW()),

-- Learning cards (in learning phase)
(4003, 1000, 3003, 'LEARNING', 4.50, 2.10, 1, 0, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), 3, NOW(), NOW()),
(4004, 1000, 3004, 'LEARNING', 3.80, 3.20, 2, 0, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3, NOW(), NOW()),
(4005, 1000, 3005, 'LEARNING', 5.20, 1.50, 1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), 2, NOW(), NOW()),

-- Review cards (graduated, various intervals)
(4006, 1000, 3006, 'REVIEW', 4.20, 8.50, 5, 0, DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 3, NOW(), NOW()),
(4007, 1000, 3007, 'REVIEW', 3.90, 15.20, 8, 1, DATE_ADD(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 4, NOW(), NOW()),
(4008, 1000, 3008, 'REVIEW', 6.10, 25.80, 12, 2, DATE_ADD(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 3, NOW(), NOW()),

-- Due cards (need review now)
(4009, 1000, 3009, 'REVIEW', 4.80, 12.30, 6, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 12 DAY), 3, NOW(), NOW()),
(4010, 1000, 3010, 'REVIEW', 5.50, 18.70, 9, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 18 DAY), 2, NOW(), NOW()),
(4011, 1000, 3011, 'REVIEW', 3.20, 6.90, 4, 0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 7 DAY), 4, NOW(), NOW()),

-- Relearning cards (failed and being relearned)
(4012, 1000, 3012, 'RELEARNING', 7.20, 0.80, 10, 3, DATE_ADD(NOW(), INTERVAL 6 HOUR), NOW(), 1, NOW(), NOW()),
(4013, 1000, 3013, 'RELEARNING', 6.50, 1.20, 7, 2, DATE_ADD(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR), 2, NOW(), NOW()),

-- Performance testing cards (for user 1002)
(4014, 1002, 3014, 'REVIEW', 4.00, 10.00, 5, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), 3, NOW(), NOW()),
(4015, 1002, 3015, 'REVIEW', 5.00, 20.00, 8, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY), 3, NOW(), NOW()),

-- Security testing cards (for user 1003)
(4016, 1003, 3016, 'REVIEW', 8.00, 30.00, 15, 3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY), 2, NOW(), NOW());

-- =============================================================================
-- USER PARAMETERS TEST DATA - FSRS Personalization
-- =============================================================================

INSERT INTO user_parameters (id, user_id, request_retention, maximum_interval, w1, w2, w3, w4, w5, w6, w7, w8, w9, w10, w11, w12, w13, w14, w15, w16, w17, optimized_at, optimization_score, created_at, updated_at) VALUES
-- Default parameters for test user
(5000, 1000, 0.90, 36500, 0.4072, 1.1829, 3.1262, 15.4722, 7.2102, 0.5316, 1.0651, 0.0234, 1.629, 0.135, 1.0440, 2.1866, 0.0661, 0.336, 2.166, 0.127, 0.729, NULL, NULL, NOW(), NOW()),

-- Optimized parameters for performance user (simulate after optimization)
(5001, 1002, 0.85, 20000, 0.4250, 1.2100, 3.2500, 16.1000, 7.5200, 0.5800, 1.1200, 0.0280, 1.720, 0.150, 1.1000, 2.3000, 0.0720, 0.360, 2.300, 0.140, 0.780, DATE_SUB(NOW(), INTERVAL 7 DAY), 87.5, NOW(), NOW()),

-- Custom parameters for security user
(5002, 1003, 0.95, 10000, 0.3800, 1.0500, 2.9000, 14.8000, 6.8000, 0.4900, 0.9800, 0.0200, 1.500, 0.120, 0.9500, 2.0000, 0.0580, 0.310, 2.000, 0.115, 0.680, DATE_SUB(NOW(), INTERVAL 30 DAY), 92.3, NOW(), NOW());

-- =============================================================================
-- REVIEW LOGS TEST DATA - Historical Review Data
-- =============================================================================

-- Comprehensive review history for optimization testing
INSERT INTO review_logs (id, user_id, problem_id, card_id, rating, elapsed_days, review_type, reviewed_at, created_at) VALUES
-- User 1000 review history (mixed performance)
(6000, 1000, 3006, 4006, 3, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6001, 1000, 3006, 4006, 4, 3, 'REVIEW', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),
(6002, 1000, 3006, 4006, 3, 5, 'REVIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(6003, 1000, 3007, 4007, 2, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(6004, 1000, 3007, 4007, 1, 2, 'REVIEW', DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(6005, 1000, 3007, 4007, 3, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(6006, 1000, 3007, 4007, 4, 7, 'REVIEW', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

(6007, 1000, 3008, 4008, 3, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
(6008, 1000, 3008, 4008, 3, 3, 'REVIEW', DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),
(6009, 1000, 3008, 4008, 1, 8, 'REVIEW', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(6010, 1000, 3008, 4008, 2, 2, 'REVIEW', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),
(6011, 1000, 3008, 4008, 3, 4, 'REVIEW', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- Performance user review history (consistent good performance)
(6012, 1002, 3014, 4014, 4, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),
(6013, 1002, 3014, 4014, 3, 5, 'REVIEW', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(6014, 1002, 3014, 4014, 4, 8, 'REVIEW', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY)),

(6015, 1002, 3015, 4015, 3, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY)),
(6016, 1002, 3015, 4015, 3, 3, 'REVIEW', DATE_SUB(NOW(), INTERVAL 27 DAY), DATE_SUB(NOW(), INTERVAL 27 DAY)),
(6017, 1002, 3015, 4015, 4, 10, 'REVIEW', DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),
(6018, 1002, 3015, 4015, 3, 15, 'REVIEW', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- Security user review history (challenging problems, mixed results)
(6019, 1003, 3016, 4016, 2, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 40 DAY)),
(6020, 1003, 3016, 4016, 1, 2, 'REVIEW', DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY)),
(6021, 1003, 3016, 4016, 3, 1, 'REVIEW', DATE_SUB(NOW(), INTERVAL 37 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY)),
(6022, 1003, 3016, 4016, 2, 5, 'REVIEW', DATE_SUB(NOW(), INTERVAL 32 DAY), DATE_SUB(NOW(), INTERVAL 32 DAY)),
(6023, 1003, 3016, 4016, 1, 8, 'REVIEW', DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY)),
(6024, 1003, 3016, 4016, 2, 3, 'REVIEW', DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY)),
(6025, 1003, 3016, 4016, 3, 10, 'REVIEW', DATE_SUB(NOW(), INTERVAL 11 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY)),
(6026, 1003, 3016, 4016, 2, 15, 'REVIEW', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =============================================================================
-- PROBLEM NOTES TEST DATA - User Study Notes
-- =============================================================================

INSERT INTO problem_notes (id, user_id, problem_id, content, created_at, updated_at, is_deleted) VALUES
(7000, 1000, 3000, 'Remember to use hash map for O(1) lookup. Time complexity: O(n), Space: O(n)', NOW(), NOW(), false),
(7001, 1000, 3003, 'Kadane algorithm: keep track of max_ending_here and max_so_far. Reset when sum becomes negative.', NOW(), NOW(), false),
(7002, 1000, 3006, 'Use dummy node to simplify edge cases. Compare values and adjust pointers accordingly.', NOW(), NOW(), false),
(7003, 1000, 3008, 'Inorder traversal: Left -> Root -> Right. Can use recursion or stack-based iterative approach.', NOW(), NOW(), false),

(7004, 1002, 3014, 'DFS approach: mark visited cells and explore all connected land cells. Use visited matrix to avoid cycles.', NOW(), NOW(), false),
(7005, 1002, 3015, 'Topological sort using DFS and detect cycles. If cycle exists, impossible to finish all courses.', NOW(), NOW(), false),

(7006, 1003, 3016, 'Binary search on merged array indices. Tricky to handle edge cases with different array sizes.', NOW(), NOW(), false),
(7007, 1003, 3017, 'DP approach: dp[i][j] represents if s[0..i-1] matches p[0..j-1]. Handle * and . cases carefully.', NOW(), NOW(), false);

-- =============================================================================
-- REVIEW SESSIONS TEST DATA - Study Session Tracking
-- =============================================================================

INSERT INTO review_sessions (id, user_id, started_at, ended_at, total_cards, correct_cards, session_type, created_at, updated_at) VALUES
(8000, 1000, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 90 MINUTE), 5, 4, 'REGULAR_REVIEW', DATE_SUB(NOW(), INTERVAL 90 MINUTE), DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(8001, 1000, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 45 MINUTE, 8, 6, 'REGULAR_REVIEW', DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 45 MINUTE, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 45 MINUTE),
(8002, 1000, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 30 MINUTE, 3, 2, 'NEW_LEARNING', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 30 MINUTE, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 30 MINUTE),

(8003, 1002, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 30 MINUTE), 10, 9, 'REGULAR_REVIEW', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(8004, 1002, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 60 MINUTE, 15, 13, 'REGULAR_REVIEW', DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 60 MINUTE, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 60 MINUTE),

(8005, 1003, DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), 3, 1, 'DIFFICULT_PROBLEMS', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(8006, 1003, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 90 MINUTE, 7, 4, 'REGULAR_REVIEW', DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 90 MINUTE, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 90 MINUTE);

-- =============================================================================
-- INDEXES FOR TEST PERFORMANCE
-- =============================================================================

-- Ensure proper indexes exist for test performance
CREATE INDEX IF NOT EXISTS idx_fsrs_cards_user_due ON fsrs_cards(user_id, due_date);
CREATE INDEX IF NOT EXISTS idx_fsrs_cards_user_state ON fsrs_cards(user_id, state);
CREATE INDEX IF NOT EXISTS idx_review_logs_user_problem ON review_logs(user_id, problem_id);
CREATE INDEX IF NOT EXISTS idx_review_logs_reviewed_at ON review_logs(reviewed_at);
CREATE INDEX IF NOT EXISTS idx_problems_difficulty ON problems(difficulty);
CREATE INDEX IF NOT EXISTS idx_problems_tags ON problems(tags);

-- =============================================================================
-- TEST DATA VALIDATION
-- =============================================================================

-- Verify test data integrity
SELECT 
    'Users' as entity, COUNT(*) as count FROM users WHERE id >= 1000
UNION ALL SELECT 
    'Problems' as entity, COUNT(*) as count FROM problems WHERE id >= 3000
UNION ALL SELECT 
    'FSRS Cards' as entity, COUNT(*) as count FROM fsrs_cards WHERE id >= 4000
UNION ALL SELECT 
    'User Parameters' as entity, COUNT(*) as count FROM user_parameters WHERE id >= 5000
UNION ALL SELECT 
    'Review Logs' as entity, COUNT(*) as count FROM review_logs WHERE id >= 6000
UNION ALL SELECT 
    'Problem Notes' as entity, COUNT(*) as count FROM problem_notes WHERE id >= 7000
UNION ALL SELECT 
    'Review Sessions' as entity, COUNT(*) as count FROM review_sessions WHERE id >= 8000;