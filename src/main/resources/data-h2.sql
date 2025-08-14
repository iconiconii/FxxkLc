-- H2 Test Data for CodeTop FSRS Backend

-- Insert test companies
INSERT INTO companies (name, name_en, logo_url) VALUES 
('阿里巴巴', 'Alibaba', 'https://example.com/alibaba.png'),
('腾讯', 'Tencent', 'https://example.com/tencent.png'),
('字节跳动', 'ByteDance', 'https://example.com/bytedance.png'),
('百度', 'Baidu', 'https://example.com/baidu.png'),
('美团', 'Meituan', 'https://example.com/meituan.png');

-- Insert test problems
INSERT INTO problems (title, title_en, difficulty, frequency, acceptance_rate, tags, companies, leetcode_id, description) VALUES 
('两数之和', 'Two Sum', 'EASY', 1250, 52.3, '["数组", "哈希表"]', '["字节跳动", "阿里巴巴", "腾讯"]', 1, '给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出和为目标值 target 的那两个整数，并返回它们的数组下标。'),
('无重复字符的最长子串', 'Longest Substring Without Repeating Characters', 'MEDIUM', 963, 38.1, '["哈希表", "字符串", "滑动窗口"]', '["字节跳动", "美团", "小米"]', 3, '给定一个字符串 s ，请你找出其中不含有重复字符的最长子串的长度。'),
('LRU缓存机制', 'LRU Cache', 'MEDIUM', 781, 53.8, '["设计", "哈希表", "链表", "双向链表"]', '["字节跳动", "阿里巴巴"]', 146, '运用你所掌握的数据结构，设计和实现一个LRU (最近最少使用) 缓存机制。'),
('反转链表', 'Reverse Linked List', 'EASY', 688, 73.5, '["递归", "链表"]', '["腾讯", "百度", "京东"]', 206, '给你单链表的头节点 head ，请你反转链表，并返回反转后的链表。'),
('数组中的第K个最大元素', 'Kth Largest Element in an Array', 'MEDIUM', 537, 65.7, '["数组", "分治", "快速选择", "排序", "堆（优先队列）"]', '["字节跳动", "快手"]', 215, '给定整数数组 nums 和整数 k，请返回数组中第 k 个最大的元素。');

-- Insert test user
INSERT INTO users (username, email, password_hash, display_name, auth_provider, email_verified) VALUES 
('testuser', 'test@example.com', '$2a$12$rQ7Uj5bTBqc2oMn2hLh5TO8W5qKNPh3t3lCtcqnMb6E5Dh1aG5P5K', 'Test User', 'LOCAL', TRUE);

-- Insert test FSRS cards (assuming user_id = 1)
INSERT INTO fsrs_cards (user_id, problem_id, difficulty, stability, state, review_count, next_review) VALUES 
(1, 1, 5.0, 2.5, 2, 3, DATEADD('DAY', 1, CURRENT_TIMESTAMP)),
(1, 2, 6.2, 1.8, 1, 2, DATEADD('HOUR', 12, CURRENT_TIMESTAMP)),
(1, 3, 4.8, 3.2, 2, 4, DATEADD('DAY', 2, CURRENT_TIMESTAMP));

-- Insert test review logs
INSERT INTO review_logs (card_id, user_id, problem_id, rating, state, elapsed_days, scheduled_days, review_duration_seconds) VALUES 
(1, 1, 1, 3, 1, 1, 1, 180),
(1, 1, 1, 4, 2, 2, 2, 120),
(1, 1, 1, 3, 2, 3, 3, 150),
(2, 1, 2, 2, 1, 1, 1, 240),
(2, 1, 2, 3, 1, 1, 1, 200);

-- Insert user parameters
INSERT INTO user_parameters (user_id, parameters, review_count, performance_score) VALUES 
(1, '[0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61]', 8, 0.8850);