#!/usr/bin/env python3
"""
Final Chinese Data Fix for CodeTop Database
完全修复CodeTop数据库中文显示问题
"""

import subprocess
import random

def execute_mysql_command(command):
    """Execute MySQL command via docker with proper UTF8 support."""
    try:
        result = subprocess.run([
            'docker', 'exec', 'codetop-mysql', 'mysql', 
            '-u', 'root', '-proot', 'codetop_fsrs', 
            '--default-character-set=utf8mb4',
            '-e', f"SET NAMES utf8mb4; {command}"
        ], capture_output=True, text=True, check=True)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def main():
    print("开始修复CodeTop数据库中文显示问题...")
    print("=" * 50)
    
    # Step 1: 完全清理现有数据
    print("第一步: 清理现有数据...")
    clear_commands = [
        "DELETE FROM problem_frequency_stats WHERE stats_scope IN ('COMPANY', 'DEPARTMENT');",
        "DELETE FROM departments;",
        "DELETE FROM companies;",
        "ALTER TABLE companies AUTO_INCREMENT = 1;",
        "ALTER TABLE departments AUTO_INCREMENT = 1;"
    ]
    
    for cmd in clear_commands:
        success, output = execute_mysql_command(cmd)
        if not success:
            print(f"✗ 清理失败: {output}")
            return
    print("✓ 数据清理完成")
    
    # Step 2: 插入正确编码的公司数据
    print("\n第二步: 插入公司数据...")
    companies = [
        ('bytedance', '字节跳动', '中国领先的互联网科技公司', 'Technology'),
        ('tencent', '腾讯', '中国跨国科技集团', 'Technology'),
        ('alibaba', '阿里巴巴', '中国跨国科技集团', 'Technology'),
        ('meituan', '美团', '中国生活服务电商平台', 'Technology'),
        ('kuaishou', '快手', '中国短视频社交平台', 'Technology'),
        ('baidu', '百度', '中国跨国科技公司', 'Technology'),
        ('microsoft', '微软', '美国跨国科技公司', 'Technology'),
        ('jd', '京东', '中国电子商务公司', 'E-commerce'),
        ('huawei', '华为', '中国跨国科技公司', 'Technology'),
        ('didi', '滴滴', '中国出行服务公司', 'Transportation'),
    ]
    
    for name, display_name, description, industry in companies:
        cmd = f"""INSERT INTO companies (name, display_name, description, industry, is_active) 
                 VALUES ('{name}', '{display_name}', '{description}', '{industry}', true);"""
        success, output = execute_mysql_command(cmd)
        if success:
            print(f"✓ 插入公司: {display_name}")
        else:
            print(f"✗ 插入失败: {display_name} - {output}")
    
    # Step 3: 插入正确编码的部门数据
    print("\n第三步: 插入部门数据...")
    departments = [
        ('backend', '后端开发', '后端开发工程师，负责服务器端逻辑开发'),
        ('frontend', '前端开发', '前端开发工程师，负责用户界面开发'),
        ('algorithm', '算法工程师', '算法工程师和数据科学家'),
        ('client', '客户端开发', '移动端和桌面端客户端开发工程师'),
        ('data', '数据研发', '数据工程师和数据分析师'),
        ('qa', '测试开发', '质量保证和测试开发工程师'),
        ('swe', '软件工程师', '通用软件开发工程师')
    ]
    
    for name, display_name, description in departments:
        cmd = f"""INSERT INTO departments (name, display_name, description, is_active) 
                 VALUES ('{name}', '{display_name}', '{description}', true);"""
        success, output = execute_mysql_command(cmd)
        if success:
            print(f"✓ 插入部门: {display_name}")
        else:
            print(f"✗ 插入失败: {display_name} - {output}")
    
    # Step 4: 重新生成频率统计数据
    print("\n第四步: 生成公司级频率统计...")
    
    # 获取公司ID
    cmd = "SELECT id, name FROM companies;"
    success, output = execute_mysql_command(cmd)
    if not success:
        print(f"✗ 获取公司列表失败: {output}")
        return
    
    companies_data = []
    lines = output.strip().split('\n')[1:]  # Skip header
    for line in lines:
        if line.strip():
            parts = line.strip().split('\t')
            if len(parts) >= 2:
                companies_data.append((int(parts[0]), parts[1]))
    
    # 获取部门ID
    cmd = "SELECT id, name FROM departments;"
    success, output = execute_mysql_command(cmd)
    if not success:
        print(f"✗ 获取部门列表失败: {output}")
        return
    
    departments_data = []
    lines = output.strip().split('\n')[1:]  # Skip header
    for line in lines:
        if line.strip():
            parts = line.strip().split('\t')
            if len(parts) >= 2:
                departments_data.append((int(parts[0]), parts[1]))
    
    # 生成公司级统计数据（简化版）
    print("生成公司级频率统计数据...")
    company_stats_count = 0
    for company_id, company_name in companies_data:
        for problem_id in range(1, 11):  # Top 10 problems per company
            frequency = random.randint(200, 800)
            interview_count = random.randint(20, 100)
            success_rate = round(random.uniform(40, 80), 1)
            
            cmd = f"""INSERT INTO problem_frequency_stats 
                     (problem_id, company_id, department_id, position_id, total_frequency_score, 
                      interview_count, unique_interviewers, last_asked_date, frequency_rank, 
                      percentile, stats_scope, avg_difficulty_rating, success_rate, avg_solve_time_minutes)
                     VALUES ({problem_id}, {company_id}, NULL, NULL, {frequency}, 
                            {interview_count}, {interview_count//2}, CURDATE() - INTERVAL {random.randint(1,30)} DAY, 
                            {problem_id}, {101-problem_id*10}, 'COMPANY', {round(random.uniform(2.5, 4.0), 1)}, 
                            {success_rate}, {random.randint(20, 45)});"""
            
            success, output = execute_mysql_command(cmd)
            if success:
                company_stats_count += 1
    
    print(f"✓ 生成公司级统计数据: {company_stats_count} 条")
    
    # 生成部门级统计数据（简化版）
    print("生成部门级频率统计数据...")
    dept_stats_count = 0
    for company_id, company_name in companies_data[:3]:  # Top 3 companies
        for dept_id, dept_name in departments_data[:3]:  # Top 3 departments
            for problem_id in range(1, 6):  # Top 5 problems per department
                frequency = random.randint(150, 600)
                interview_count = random.randint(10, 50)
                success_rate = round(random.uniform(35, 75), 1)
                
                cmd = f"""INSERT INTO problem_frequency_stats 
                         (problem_id, company_id, department_id, position_id, total_frequency_score, 
                          interview_count, unique_interviewers, last_asked_date, frequency_rank, 
                          percentile, stats_scope, avg_difficulty_rating, success_rate, avg_solve_time_minutes)
                         VALUES ({problem_id}, {company_id}, {dept_id}, NULL, {frequency}, 
                                {interview_count}, {interview_count//3}, CURDATE() - INTERVAL {random.randint(1,60)} DAY, 
                                {problem_id}, {101-problem_id*20}, 'DEPARTMENT', {round(random.uniform(2.8, 4.2), 1)}, 
                                {success_rate}, {random.randint(25, 50)});"""
                
                success, output = execute_mysql_command(cmd)
                if success:
                    dept_stats_count += 1
    
    print(f"✓ 生成部门级统计数据: {dept_stats_count} 条")
    
    # Step 5: 验证修复结果
    print("\n第五步: 验证中文显示效果...")
    
    # 验证公司中文显示
    cmd = "SELECT name as '英文代码', display_name as '中文名称', industry as '行业' FROM companies LIMIT 5;"
    success, output = execute_mysql_command(cmd)
    if success:
        print("\n✓ 公司中文显示测试:")
        print(output)
    
    # 验证部门中文显示
    cmd = "SELECT name as '英文代码', display_name as '中文名称', description as '描述' FROM departments LIMIT 5;"
    success, output = execute_mysql_command(cmd)
    if success:
        print("✓ 部门中文显示测试:")
        print(output)
    
    # 验证综合查询
    cmd = """SELECT 
                c.display_name as '公司',
                d.display_name as '部门', 
                COUNT(pfs.id) as '题目数',
                ROUND(AVG(pfs.total_frequency_score), 1) as '平均频率'
             FROM problem_frequency_stats pfs
             JOIN companies c ON pfs.company_id = c.id
             JOIN departments d ON pfs.department_id = d.id
             WHERE pfs.stats_scope = 'DEPARTMENT'
             GROUP BY c.id, d.id, c.display_name, d.display_name
             ORDER BY AVG(pfs.total_frequency_score) DESC
             LIMIT 5;"""
    
    success, output = execute_mysql_command(cmd)
    if success:
        print("✓ 综合查询中文显示测试:")
        print(output)
    
    # 统计总结
    cmd = """SELECT 
                '公司数量' as '数据类型', COUNT(*) as '数量' FROM companies
             UNION ALL
             SELECT 
                '部门数量' as '数据类型', COUNT(*) as '数量' FROM departments
             UNION ALL
             SELECT 
                '频率统计记录' as '数据类型', COUNT(*) as '数量' FROM problem_frequency_stats;"""
    
    success, output = execute_mysql_command(cmd)
    if success:
        print("\n" + "=" * 50)
        print("修复完成统计:")
        print(output)
    
    print("\n🎉 CodeTop数据库中文显示问题修复完成！")
    print("📋 修复内容:")
    print(f"   - 重新创建了 {len(companies)} 个公司的中文数据")
    print(f"   - 重新创建了 {len(departments)} 个部门的中文数据") 
    print(f"   - 生成了 {company_stats_count} 条公司级频率统计")
    print(f"   - 生成了 {dept_stats_count} 条部门级频率统计")
    print("✨ 现在所有中文内容都应该正确显示！")

if __name__ == "__main__":
    random.seed(42)
    main()