#!/usr/bin/env python3
"""
CodeTop数据获取脚本
获取完整的1134道题目数据并生成SQL插入语句
"""

import requests
import json
import time
from datetime import datetime
from typing import List, Dict, Any

def fetch_all_problems() -> Dict[str, Any]:
    """
    从CodeTop API获取所有题目数据
    """
    url = "https://codetop.cc/api/questions"
    headers = {
        'Accept': 'application/json',
        'User-Agent': 'Mozilla/5.0 (compatible; CodeTopDataFetcher/1.0)',
        'Referer': 'https://codetop.cc/home'
    }
    
    print(f"正在获取数据: {url}")
    try:
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        
        data = response.json()
        print(f"成功获取数据: 总共{data.get('count', 0)}道题目")
        return data
        
    except requests.RequestException as e:
        print(f"请求失败: {e}")
        return {}
    except json.JSONDecodeError as e:
        print(f"JSON解析失败: {e}")
        return {}

def map_difficulty(level: int) -> str:
    """
    映射难度等级
    1 = EASY, 2 = MEDIUM, 3 = HARD
    """
    mapping = {1: 'EASY', 2: 'MEDIUM', 3: 'HARD'}
    return mapping.get(level, 'MEDIUM')

def clean_html(content: str) -> str:
    """
    清理HTML内容，保留基本信息
    """
    if not content:
        return ""
    # 简单的HTML清理，保留重要信息
    import re
    # 移除HTML标签但保留内容
    cleaned = re.sub(r'<[^>]+>', '', content)
    # 清理多余的空白字符
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    return cleaned[:200] + "..." if len(cleaned) > 200 else cleaned

def format_date(date_str: str) -> str:
    """
    格式化日期字符串为 YYYY-MM-DD 格式
    """
    try:
        # 解析ISO格式的日期时间
        dt = datetime.fromisoformat(date_str.replace('Z', '+00:00'))
        return dt.strftime('%Y-%m-%d')
    except:
        return '2025-08-01'

def classify_tags(title: str, difficulty: str) -> List[str]:
    """
    根据题目标题和难度推断标签
    """
    tags = []
    
    # 基于标题关键词推断标签
    title_lower = title.lower()
    
    if any(word in title for word in ['数组', '最大', '最小', '第K个', '合并', '排序']):
        tags.append('数组')
    if any(word in title for word in ['字符串', '子串', '回文']):
        tags.append('字符串')
    if any(word in title for word in ['链表', '反转', '合并']):
        tags.append('链表')
    if any(word in title for word in ['二叉树', '树', '遍历']):
        tags.append('树')
    if any(word in title for word in ['哈希', 'LRU', '缓存']):
        tags.append('哈希表')
    if any(word in title for word in ['搜索', '查找', '旋转']):
        tags.append('二分查找')
    if any(word in title for word in ['岛屿', '图']):
        tags.append('深度优先搜索')
    if any(word in title for word in ['层序', '层次']):
        tags.append('广度优先搜索')
    if any(word in title for word in ['排列', '组合']):
        tags.append('回溯算法')
    if any(word in title for word in ['动态', '最优', '最长', '最大和']):
        tags.append('动态规划')
    if any(word in title for word in ['栈', '括号']):
        tags.append('栈')
    if any(word in title for word in ['快速排序', '排序']):
        tags.append('排序')
    if any(word in title for word in ['双指针', '三数之和', '两数之和']):
        tags.append('双指针')
    
    # 如果没有推断出标签，根据难度给默认标签
    if not tags:
        if difficulty == 'EASY':
            tags.append('基础算法')
        elif difficulty == 'HARD':
            tags.append('高级算法')
        else:
            tags.append('算法')
    
    return tags

def get_frequency_level(frequency: int) -> str:
    """
    根据频度数值返回频度等级
    """
    if frequency >= 600:
        return 'VERY_HIGH'
    elif frequency >= 400:
        return 'HIGH'
    elif frequency >= 200:
        return 'MEDIUM'
    else:
        return 'LOW'

def generate_problem_sql(problems: List[Dict[str, Any]]) -> str:
    """
    生成题目数据的SQL插入语句
    """
    sql_lines = []
    sql_lines.append("-- CodeTop完整题目数据 (基于API真实数据)")
    sql_lines.append(f"-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    sql_lines.append(f"-- 总题目数量: {len(problems)}")
    sql_lines.append("")
    sql_lines.append("INSERT INTO problems (title, difficulty, problem_url, leetcode_id, tags, is_premium, is_active) VALUES")
    
    problem_values = []
    
    for i, item in enumerate(problems):
        try:
            leetcode_info = item.get('leetcode', {})
            
            title = leetcode_info.get('title', '').replace("'", "''")  # SQL转义
            leetcode_id = leetcode_info.get('frontend_question_id', str(i+1))
            difficulty = map_difficulty(leetcode_info.get('level', 2))
            slug = leetcode_info.get('slug_title', '')
            
            # 构建LeetCode URL
            if slug:
                problem_url = f"https://leetcode.cn/problems/{slug}"
            else:
                problem_url = f"https://leetcode.cn/problems/problem-{leetcode_id}"
            
            # 推断标签
            tags = classify_tags(title, difficulty)
            tags_json = "JSON_ARRAY(" + ", ".join([f"'{tag}'" for tag in tags]) + ")"
            
            # 判断是否为付费题目 (简单判断逻辑)
            is_premium = 'true' if 'premium' in title.lower() or '付费' in title else 'false'
            
            problem_value = f"('{title}', '{difficulty}', '{problem_url}', '{leetcode_id}', {tags_json}, {is_premium}, true)"
            problem_values.append(problem_value)
            
        except Exception as e:
            print(f"处理第{i+1}个题目时出错: {e}")
            continue
    
    # 分批插入，每批100条
    batch_size = 100
    for i in range(0, len(problem_values), batch_size):
        batch = problem_values[i:i+batch_size]
        if i > 0:
            sql_lines.append("\nINSERT INTO problems (title, difficulty, problem_url, leetcode_id, tags, is_premium, is_active) VALUES")
        
        for j, value in enumerate(batch):
            if j == len(batch) - 1:
                sql_lines.append(f"{value}")
            else:
                sql_lines.append(f"{value},")
        
        sql_lines.append("ON DUPLICATE KEY UPDATE")
        sql_lines.append("title = VALUES(title),")
        sql_lines.append("difficulty = VALUES(difficulty),")
        sql_lines.append("problem_url = VALUES(problem_url),")
        sql_lines.append("tags = VALUES(tags),")
        sql_lines.append("updated_at = CURRENT_TIMESTAMP;")
        sql_lines.append("")
    
    return "\n".join(sql_lines)

def generate_frequency_sql(problems: List[Dict[str, Any]]) -> str:
    """
    生成频度关联数据的SQL
    """
    sql_lines = []
    sql_lines.append("-- 题目-公司关联数据 (基于真实频度)")
    sql_lines.append("INSERT INTO problem_companies (problem_id, company_id, frequency, times_asked, last_asked_date)")
    sql_lines.append("SELECT")
    sql_lines.append("    p.id as problem_id,")
    sql_lines.append("    c.id as company_id,")
    sql_lines.append("    CASE")
    
    # 生成频度映射
    for item in problems[:50]:  # 只处理前50个高频题目
        try:
            leetcode_info = item.get('leetcode', {})
            leetcode_id = leetcode_info.get('frontend_question_id', '')
            frequency = item.get('value', 0)
            frequency_level = get_frequency_level(frequency)
            last_exam_date = format_date(item.get('time', ''))
            
            sql_lines.append(f"        WHEN p.leetcode_id = '{leetcode_id}' THEN '{frequency_level}'")
            
        except Exception as e:
            continue
    
    sql_lines.append("        ELSE 'LOW'")
    sql_lines.append("    END as frequency,")
    sql_lines.append("    CASE")
    
    # 生成真实频度数值
    for item in problems[:50]:
        try:
            leetcode_info = item.get('leetcode', {})
            leetcode_id = leetcode_info.get('frontend_question_id', '')
            frequency = item.get('value', 0)
            
            sql_lines.append(f"        WHEN p.leetcode_id = '{leetcode_id}' THEN {frequency}")
            
        except Exception as e:
            continue
    
    sql_lines.append("        ELSE 50")
    sql_lines.append("    END as times_asked,")
    sql_lines.append("    CASE")
    
    # 生成考察日期
    for item in problems[:50]:
        try:
            leetcode_info = item.get('leetcode', {})
            leetcode_id = leetcode_info.get('frontend_question_id', '')
            last_exam_date = format_date(item.get('time', ''))
            
            sql_lines.append(f"        WHEN p.leetcode_id = '{leetcode_id}' THEN '{last_exam_date}'")
            
        except Exception as e:
            continue
    
    sql_lines.append("        ELSE '2025-01-01'")
    sql_lines.append("    END as last_asked_date")
    sql_lines.append("FROM problems p")
    sql_lines.append("CROSS JOIN companies c")
    sql_lines.append("WHERE c.name IN ('bytedance', 'alibaba', 'tencent', 'baidu', 'microsoft', 'meituan')")
    sql_lines.append("ON DUPLICATE KEY UPDATE")
    sql_lines.append("frequency = VALUES(frequency),")
    sql_lines.append("times_asked = VALUES(times_asked),")
    sql_lines.append("last_asked_date = VALUES(last_asked_date),")
    sql_lines.append("updated_at = CURRENT_TIMESTAMP;")
    
    return "\n".join(sql_lines)

def main():
    print("=== CodeTop数据获取工具 ===")
    
    # 获取数据
    data = fetch_all_problems()
    if not data or 'list' not in data:
        print("❌ 数据获取失败")
        return
    
    problems = data['list']
    total_count = data.get('count', len(problems))
    
    print(f"✅ 成功获取 {len(problems)} 道题目数据 (总计: {total_count})")
    
    # 按频度排序
    problems.sort(key=lambda x: x.get('value', 0), reverse=True)
    
    # 生成SQL
    print("📝 生成SQL文件...")
    
    # 生成题目数据SQL
    problems_sql = generate_problem_sql(problems)
    
    # 生成频度关联SQL  
    frequency_sql = generate_frequency_sql(problems)
    
    # 写入完整SQL文件
    complete_sql = f"""-- CodeTop完整数据库导入文件
-- 数据来源: https://codetop.cc/api/questions
-- 总题目数量: {len(problems)}
-- 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

-- ===================================
-- 1. 插入公司数据
-- ===================================

INSERT INTO companies (name, display_name, description, industry, is_active) VALUES
('bytedance', '字节跳动', 'Chinese multinational internet technology company', 'Technology', true),
('microsoft', '微软', 'American multinational technology corporation', 'Technology', true),
('meituan', '美团', 'Chinese food delivery and lifestyle platform', 'Technology', true),
('alibaba', '阿里巴巴', 'Chinese multinational technology conglomerate', 'Technology', true),
('kuaishou', '快手', 'Chinese video sharing mobile app', 'Technology', true),
('tencent', '腾讯', 'Chinese multinational technology conglomerate', 'Technology', true),
('yuanfudao', '猿辅导', 'Chinese online education platform', 'Education', true),
('baidu', '百度', 'Chinese multinational technology company', 'Technology', true),
('didi', '滴滴', 'Chinese vehicle for hire company', 'Transportation', true),
('jd', '京东', 'Chinese e-commerce company', 'E-commerce', true),
('huawei', '华为', 'Chinese multinational technology corporation', 'Technology', true),
('pdd', '拼多多', 'Chinese e-commerce platform', 'E-commerce', true),
('netease', '网易', 'Chinese technology company', 'Technology', true),
('xiaomi', '小米', 'Chinese electronics company', 'Technology', true),
('amazon', '亚马逊', 'American multinational technology company', 'Technology', true),
('shopee', '虾皮', 'Singaporean e-commerce platform', 'E-commerce', true),
('ctrip', '携程', 'Chinese travel services company', 'Travel', true),
('bilibili', 'bilibili', 'Chinese video sharing website', 'Entertainment', true)
ON DUPLICATE KEY UPDATE 
display_name = VALUES(display_name),
description = VALUES(description),
industry = VALUES(industry),
is_active = VALUES(is_active),
updated_at = CURRENT_TIMESTAMP;

-- ===================================
-- 2. 插入题目数据 (共{len(problems)}道题目)
-- ===================================

{problems_sql}

-- ===================================
-- 3. 插入题目-公司关联数据
-- ===================================

{frequency_sql}

-- ===================================
-- 4. 创建统计视图
-- ===================================

CREATE OR REPLACE VIEW problem_frequency_stats AS
SELECT 
    p.leetcode_id,
    p.title,
    p.difficulty,
    p.problem_url,
    AVG(pc.times_asked) as avg_frequency,
    MAX(pc.times_asked) as max_frequency,
    COUNT(DISTINCT pc.company_id) as company_count,
    MAX(pc.last_asked_date) as latest_exam_date
FROM problems p
LEFT JOIN problem_companies pc ON p.id = pc.problem_id
WHERE p.deleted = 0 AND p.is_active = 1
GROUP BY p.id, p.leetcode_id, p.title, p.difficulty, p.problem_url
ORDER BY avg_frequency DESC;

-- ===================================
-- 5. 数据统计验证
-- ===================================

SELECT 
    '总题目数量' as metric,
    COUNT(*) as count
FROM problems 
WHERE deleted = 0 AND is_active = 1;

SELECT 
    difficulty,
    COUNT(*) as count
FROM problems 
WHERE deleted = 0 AND is_active = 1
GROUP BY difficulty
ORDER BY 
    CASE difficulty 
        WHEN 'EASY' THEN 1 
        WHEN 'MEDIUM' THEN 2 
        WHEN 'HARD' THEN 3 
    END;

-- 显示频度最高的前10道题
SELECT 
    p.leetcode_id,
    p.title,
    p.difficulty,
    AVG(pc.times_asked) as avg_frequency
FROM problems p
LEFT JOIN problem_companies pc ON p.id = pc.problem_id
WHERE p.deleted = 0 AND p.is_active = 1
GROUP BY p.id, p.leetcode_id, p.title, p.difficulty
ORDER BY avg_frequency DESC
LIMIT 10;
"""

    # 保存文件
    output_file = '/mnt/e/FxxkLC/codetop_complete_data.sql'
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(complete_sql)
    
    print(f"✅ SQL文件已生成: {output_file}")
    print(f"📊 数据统计:")
    print(f"   - 总题目数量: {len(problems)}")
    
    # 统计难度分布
    difficulty_count = {}
    for problem in problems:
        level = problem.get('leetcode', {}).get('level', 2)
        diff = map_difficulty(level)
        difficulty_count[diff] = difficulty_count.get(diff, 0) + 1
    
    for diff, count in difficulty_count.items():
        print(f"   - {diff}: {count}道")
    
    # 显示频度最高的前5道题
    print(f"\n🔥 频度最高的前5道题:")
    for i, problem in enumerate(problems[:5]):
        leetcode_info = problem.get('leetcode', {})
        title = leetcode_info.get('title', '')
        leetcode_id = leetcode_info.get('frontend_question_id', '')
        frequency = problem.get('value', 0)
        print(f"   {i+1}. {leetcode_id}. {title} (频度: {frequency})")

if __name__ == "__main__":
    main()