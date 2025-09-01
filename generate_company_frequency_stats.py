#!/usr/bin/env python3
"""
Generate comprehensive company and department-level frequency statistics.
Based on data from CodeTop website and existing database structure.
"""

import subprocess
import random
import json
from datetime import datetime, date, timedelta

# Company data from CodeTop
COMPANIES = [
    ('bytedance', '字节跳动', 'Technology'),
    ('microsoft', '微软', 'Technology'), 
    ('meituan', '美团', 'Technology'),
    ('alibaba', '阿里巴巴', 'Technology'),
    ('kuaishou', '快手', 'Technology'),
    ('tencent', '腾讯', 'Technology'),
    ('yuanfudao', '猿辅导', 'Education'),
    ('baidu', '百度', 'Technology'),
    ('didi', '滴滴', 'Transportation'),
    ('jd', '京东', 'E-commerce'),
    ('huawei', '华为', 'Technology'),
    ('pdd', '拼多多', 'E-commerce'),
    ('netease', '网易', 'Technology'),
    ('xiaomi', '小米', 'Technology'),
    ('shangtang', '商汤', 'AI'),
    ('megvii', '旷视', 'AI'),
    ('amazon', '亚马逊', 'Technology'),
    ('shopee', '虾皮', 'E-commerce'),
    ('tusimple', '图森', 'Autonomous Driving'),
    ('ctrip', '携程', 'Travel'),
    ('bilibili', 'bilibili', 'Entertainment'),
    ('xiaohongshu', '小红书', 'Social Media')
]

# Department/Position data from CodeTop
DEPARTMENTS = [
    ('backend', '后端', 'Backend development'),
    ('frontend', '前端', 'Frontend development'), 
    ('client', '客户端', 'Mobile/Desktop client development'),
    ('algorithm', '算法', 'Algorithm and data science'),
    ('data', '数据研发', 'Data engineering and analytics'),
    ('qa', '测试', 'Quality assurance and testing'),
    ('swe', 'Software Engineer', 'General software engineering')
]

# Top 100 problems with their global frequency scores
PROBLEM_FREQUENCIES = [
    (1, 976), (2, 790), (3, 694), (4, 545), (5, 454), (6, 426), (7, 349), (8, 316), (9, 304), (10, 296),
    (11, 285), (12, 285), (13, 281), (14, 276), (15, 270), (16, 263), (17, 262), (18, 252), (19, 247), (20, 246),
    (21, 243), (22, 239), (23, 230), (24, 229), (25, 219), (26, 218), (27, 209), (28, 208), (29, 207), (30, 202),
    (31, 199), (32, 198), (33, 195), (34, 190), (35, 189), (36, 188), (37, 186), (38, 185), (39, 182), (40, 180),
    (41, 178), (42, 177), (43, 175), (44, 173), (45, 171), (46, 170), (47, 168), (48, 166), (49, 165), (50, 163),
    (51, 161), (52, 160), (53, 158), (54, 156), (55, 155), (56, 153), (57, 151), (58, 150), (59, 148), (60, 146),
    (61, 145), (62, 143), (63, 141), (64, 140), (65, 138), (66, 136), (67, 135), (68, 133), (69, 131), (70, 130),
    (71, 128), (72, 126), (73, 125), (74, 123), (75, 121), (76, 120), (77, 118), (78, 116), (79, 115), (80, 113),
    (81, 111), (82, 110), (83, 108), (84, 106), (85, 105), (86, 103), (87, 101), (88, 100), (89, 98), (90, 96),
    (91, 95), (92, 93), (93, 91), (94, 90), (95, 88), (96, 86), (97, 85), (98, 83), (99, 81), (100, 79)
]

def execute_mysql_command(command):
    """Execute MySQL command via docker."""
    try:
        result = subprocess.run([
            'docker', 'exec', 'codetop-mysql', 'mysql', '-u', 'root', '-proot', 'codetop_fsrs', '-e', command
        ], capture_output=True, text=True, check=True)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def get_company_frequency_variation(global_freq, company_name):
    """Generate company-specific frequency variation based on company characteristics."""
    # Different companies have different problem preferences
    company_multipliers = {
        'bytedance': 1.2,  # High frequency for bytedance (very active in hiring)
        'alibaba': 1.1,   # High frequency for alibaba
        'tencent': 1.15,  # High frequency for tencent
        'microsoft': 0.9, # Slightly lower frequency for foreign companies
        'amazon': 0.8,    # Lower frequency for foreign companies
        'meituan': 1.05,  # Moderate frequency
        'kuaishou': 1.0,  # Average frequency
        'baidu': 0.95,    # Slightly below average
        'huawei': 1.0,    # Average frequency
        'jd': 0.9,        # Slightly below average
        'didi': 0.85,     # Lower frequency
        'pdd': 1.1,       # High frequency (growing company)
        'xiaomi': 0.9,    # Moderate frequency
        'netease': 0.85,  # Lower frequency
        'shopee': 0.8,    # Lower frequency (regional)
        'bilibili': 0.75, # Lower frequency (entertainment)
        'xiaohongshu': 0.7, # Lower frequency (social media)
    }
    
    multiplier = company_multipliers.get(company_name, 0.8)
    
    # Add some randomization
    variation = random.uniform(0.8, 1.2)
    company_freq = int(global_freq * multiplier * variation)
    
    return max(1, company_freq)  # Ensure minimum frequency of 1

def get_department_frequency_variation(company_freq, dept_name):
    """Generate department-specific frequency variation."""
    # Different departments focus on different types of problems
    dept_multipliers = {
        'backend': 1.0,      # Standard frequency
        'frontend': 0.7,     # Lower frequency (fewer algorithm questions)
        'client': 0.8,       # Moderate frequency
        'algorithm': 1.3,    # Higher frequency (algorithm-focused)
        'data': 1.1,         # Slightly higher frequency
        'qa': 0.6,           # Lower frequency (fewer algorithm questions)
        'swe': 0.9,          # Slightly below average
    }
    
    multiplier = dept_multipliers.get(dept_name, 0.8)
    variation = random.uniform(0.85, 1.15)
    dept_freq = int(company_freq * multiplier * variation)
    
    return max(1, dept_freq)

def generate_realistic_metrics(frequency_score, scope_type, rank_within_scope):
    """Generate realistic interview metrics based on frequency and scope."""
    # Base calculations
    base_interviews = max(1, int(frequency_score / 8))  # Slightly lower than global
    interview_count = base_interviews + random.randint(-3, 3)
    interview_count = max(1, interview_count)
    
    # Unique interviewers (roughly 60% of interviews for companies, 40% for departments)
    if scope_type == 'COMPANY':
        unique_ratio = 0.6
    else:  # DEPARTMENT/POSITION
        unique_ratio = 0.4
        
    unique_interviewers = max(1, int(interview_count * unique_ratio) + random.randint(-1, 1))
    unique_interviewers = max(1, min(unique_interviewers, interview_count))
    
    # Calculate percentile within scope
    percentile = round(max(1.0, 101 - rank_within_scope), 2)
    
    # Generate difficulty rating (companies have more varied difficulty)
    if scope_type == 'COMPANY':
        avg_difficulty = round(random.uniform(2.2, 4.0), 1)
    else:  # DEPARTMENT/POSITION  
        avg_difficulty = round(random.uniform(2.5, 4.2), 1)
    
    # Generate success rate
    if avg_difficulty <= 2.5:
        success_rate = round(random.uniform(65, 85), 1)
    elif avg_difficulty <= 3.5:
        success_rate = round(random.uniform(45, 70), 1)
    else:
        success_rate = round(random.uniform(25, 55), 1)
    
    # Generate solve time
    if avg_difficulty <= 2.5:
        solve_time = random.randint(12, 22)
    elif avg_difficulty <= 3.5:
        solve_time = random.randint(22, 38)
    else:
        solve_time = random.randint(35, 55)
    
    return {
        'interview_count': interview_count,
        'unique_interviewers': unique_interviewers,
        'percentile': percentile,
        'avg_difficulty_rating': avg_difficulty,
        'success_rate': success_rate,
        'avg_solve_time_minutes': solve_time
    }

def setup_companies_and_departments():
    """Insert companies and departments data."""
    print("Setting up companies and departments...")
    
    # Insert companies
    for company_code, company_name, industry in COMPANIES:
        cmd = f"""
        INSERT INTO companies (name, display_name, description, industry, is_active) 
        VALUES ('{company_code}', '{company_name}', '{industry} company', '{industry}', true)
        ON DUPLICATE KEY UPDATE 
        display_name = VALUES(display_name),
        description = VALUES(description),
        industry = VALUES(industry),
        updated_at = CURRENT_TIMESTAMP;
        """
        success, output = execute_mysql_command(cmd)
        if not success:
            print(f"✗ Failed to insert company {company_name}: {output}")
    
    # Insert departments
    for dept_code, dept_name, description in DEPARTMENTS:
        cmd = f"""
        INSERT INTO departments (name, display_name, description, is_active) 
        VALUES ('{dept_code}', '{dept_name}', '{description}', true)
        ON DUPLICATE KEY UPDATE 
        display_name = VALUES(display_name),
        description = VALUES(description),
        updated_at = CURRENT_TIMESTAMP;
        """
        success, output = execute_mysql_command(cmd)
        if not success:
            print(f"✗ Failed to insert department {dept_name}: {output}")
    
    print("✓ Companies and departments setup completed")

def generate_company_frequency_stats():
    """Generate frequency statistics for company scope."""
    print("Generating company-level frequency statistics...")
    
    # Get company IDs
    cmd = "SELECT id, name, display_name FROM companies WHERE is_active = true;"
    success, output = execute_mysql_command(cmd)
    if not success:
        print(f"✗ Failed to get companies: {output}")
        return
    
    companies = []
    lines = output.strip().split('\n')[1:]  # Skip header
    for line in lines:
        if line.strip():
            parts = line.strip().split('\t')
            if len(parts) >= 3:
                companies.append((int(parts[0]), parts[1], parts[2]))
    
    print(f"Found {len(companies)} companies")
    
    # Generate stats for each company and problem combination
    total_generated = 0
    for company_id, company_code, company_name in companies[:10]:  # Limit to first 10 companies for demo
        print(f"Processing company: {company_name}")
        
        # Generate frequency stats for top 20 problems for this company
        company_problems = []
        for problem_id, global_freq in PROBLEM_FREQUENCIES[:20]:  # Top 20 problems per company
            company_freq = get_company_frequency_variation(global_freq, company_code)
            company_problems.append((problem_id, company_freq))
        
        # Sort by frequency and assign ranks
        company_problems.sort(key=lambda x: x[1], reverse=True)
        
        # Insert company-level stats
        values = []
        for rank, (problem_id, frequency_score) in enumerate(company_problems, 1):
            metrics = generate_realistic_metrics(frequency_score, 'COMPANY', rank)
            
            # Random recent date
            days_ago = random.randint(1, 30)
            last_asked = f"DATE_SUB(CURDATE(), INTERVAL {days_ago} DAY)"
            
            value = f"""({problem_id}, {company_id}, NULL, NULL, {frequency_score}, 
                       {metrics['interview_count']}, {metrics['unique_interviewers']}, 
                       {last_asked}, DATE_SUB(CURDATE(), INTERVAL 120 DAY), 'STABLE', 
                       {metrics['avg_difficulty_rating']}, {metrics['success_rate']}, 
                       {metrics['avg_solve_time_minutes']}, {rank}, {metrics['percentile']}, 
                       'COMPANY')"""
            values.append(value)
        
        # Insert in batches of 10
        for i in range(0, len(values), 10):
            batch = values[i:i+10]
            insert_cmd = f"""INSERT INTO problem_frequency_stats (
                problem_id, company_id, department_id, position_id, total_frequency_score, 
                interview_count, unique_interviewers, last_asked_date, first_asked_date, 
                frequency_trend, avg_difficulty_rating, success_rate, avg_solve_time_minutes, 
                frequency_rank, percentile, stats_scope
            ) VALUES {', '.join(batch)};"""
            
            success, output = execute_mysql_command(insert_cmd)
            if success:
                total_generated += len(batch)
                print(f"  ✓ Inserted batch for {company_name}: {len(batch)} records")
            else:
                print(f"  ✗ Failed batch for {company_name}: {output}")
    
    print(f"✓ Generated {total_generated} company-level frequency statistics")

def generate_department_frequency_stats():
    """Generate frequency statistics for department scope."""
    print("Generating department-level frequency statistics...")
    
    # Get company and department IDs
    cmd_companies = "SELECT id, name FROM companies WHERE is_active = true LIMIT 5;"
    success, output = execute_mysql_command(cmd_companies)
    if not success:
        print(f"✗ Failed to get companies: {output}")
        return
    
    companies = []
    lines = output.strip().split('\n')[1:]  # Skip header
    for line in lines:
        if line.strip():
            parts = line.strip().split('\t')
            if len(parts) >= 2:
                companies.append((int(parts[0]), parts[1]))
    
    cmd_departments = "SELECT id, name FROM departments WHERE is_active = true;"
    success, output = execute_mysql_command(cmd_departments)
    if not success:
        print(f"✗ Failed to get departments: {output}")
        return
    
    departments = []
    lines = output.strip().split('\n')[1:]  # Skip header
    for line in lines:
        if line.strip():
            parts = line.strip().split('\t')
            if len(parts) >= 2:
                departments.append((int(parts[0]), parts[1]))
    
    print(f"Found {len(companies)} companies and {len(departments)} departments")
    
    # Generate stats for company-department combinations
    total_generated = 0
    for company_id, company_code in companies:
        for dept_id, dept_code in departments[:3]:  # Limit to 3 departments per company
            print(f"Processing company {company_code} - department {dept_code}")
            
            # Generate frequency stats for top 10 problems for this company-department combo
            dept_problems = []
            for problem_id, global_freq in PROBLEM_FREQUENCIES[:10]:  # Top 10 problems per dept
                company_freq = get_company_frequency_variation(global_freq, company_code)
                dept_freq = get_department_frequency_variation(company_freq, dept_code)
                dept_problems.append((problem_id, dept_freq))
            
            # Sort by frequency and assign ranks
            dept_problems.sort(key=lambda x: x[1], reverse=True)
            
            # Insert department-level stats
            values = []
            for rank, (problem_id, frequency_score) in enumerate(dept_problems, 1):
                metrics = generate_realistic_metrics(frequency_score, 'DEPARTMENT', rank)
                
                # Random recent date
                days_ago = random.randint(1, 60)
                last_asked = f"DATE_SUB(CURDATE(), INTERVAL {days_ago} DAY)"
                
                value = f"""({problem_id}, {company_id}, {dept_id}, NULL, {frequency_score}, 
                           {metrics['interview_count']}, {metrics['unique_interviewers']}, 
                           {last_asked}, DATE_SUB(CURDATE(), INTERVAL 90 DAY), 'STABLE', 
                           {metrics['avg_difficulty_rating']}, {metrics['success_rate']}, 
                           {metrics['avg_solve_time_minutes']}, {rank}, {metrics['percentile']}, 
                           'DEPARTMENT')"""
                values.append(value)
            
            # Insert batch
            insert_cmd = f"""INSERT INTO problem_frequency_stats (
                problem_id, company_id, department_id, position_id, total_frequency_score, 
                interview_count, unique_interviewers, last_asked_date, first_asked_date, 
                frequency_trend, avg_difficulty_rating, success_rate, avg_solve_time_minutes, 
                frequency_rank, percentile, stats_scope
            ) VALUES {', '.join(values)};"""
            
            success, output = execute_mysql_command(insert_cmd)
            if success:
                total_generated += len(values)
                print(f"  ✓ Inserted {len(values)} records for {company_code}-{dept_code}")
            else:
                print(f"  ✗ Failed for {company_code}-{dept_code}: {output}")
    
    print(f"✓ Generated {total_generated} department-level frequency statistics")

def main():
    print("Starting comprehensive frequency statistics generation...")
    print("=" * 60)
    
    random.seed(42)  # For reproducible results
    
    # Step 1: Setup companies and departments
    setup_companies_and_departments()
    
    # Step 2: Clear existing non-global stats
    print("\nClearing existing company/department frequency stats...")
    clear_cmd = "DELETE FROM problem_frequency_stats WHERE stats_scope IN ('COMPANY', 'DEPARTMENT', 'POSITION');"
    success, output = execute_mysql_command(clear_cmd)
    if success:
        print("✓ Cleared existing company/department stats")
    else:
        print(f"✗ Failed to clear existing stats: {output}")
        return
    
    # Step 3: Generate company-level stats
    generate_company_frequency_stats()
    
    # Step 4: Generate department-level stats
    generate_department_frequency_stats()
    
    # Step 5: Verification
    print("\n" + "=" * 60)
    print("VERIFICATION RESULTS:")
    
    # Check totals by scope
    verify_cmd = """
    SELECT 
        stats_scope,
        COUNT(*) as total_records,
        COUNT(DISTINCT company_id) as unique_companies,
        COUNT(DISTINCT department_id) as unique_departments,
        ROUND(AVG(total_frequency_score), 2) as avg_frequency,
        MIN(total_frequency_score) as min_frequency,
        MAX(total_frequency_score) as max_frequency
    FROM problem_frequency_stats 
    WHERE stats_scope IN ('GLOBAL', 'COMPANY', 'DEPARTMENT')
    GROUP BY stats_scope
    ORDER BY 
        CASE stats_scope 
            WHEN 'GLOBAL' THEN 1 
            WHEN 'COMPANY' THEN 2 
            WHEN 'DEPARTMENT' THEN 3 
        END;
    """
    success, output = execute_mysql_command(verify_cmd)
    if success:
        print(output)
    
    # Check sample company data
    sample_cmd = """
    SELECT 
        c.display_name as company_name,
        COUNT(pfs.id) as problem_count,
        ROUND(AVG(pfs.total_frequency_score), 2) as avg_frequency,
        MAX(pfs.total_frequency_score) as max_frequency
    FROM problem_frequency_stats pfs
    JOIN companies c ON pfs.company_id = c.id
    WHERE pfs.stats_scope = 'COMPANY'
    GROUP BY c.id, c.display_name
    ORDER BY avg_frequency DESC
    LIMIT 5;
    """
    success, output = execute_mysql_command(sample_cmd)
    if success:
        print("\nTop 5 companies by average frequency:")
        print(output)
    
    print("\n✓ Comprehensive frequency statistics generation completed!")

if __name__ == "__main__":
    main()