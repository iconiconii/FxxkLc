#!/usr/bin/env python3
"""
Generate SQL INSERT statements for problem_frequency_stats table.
This script generates the data based on the frequency values from the original SQL file.
"""

import subprocess
import random

# Frequency data extracted from codetop_comprehensive_problems_100.sql
# Each tuple: (problem_id, frequency_score)
frequency_data = [
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

def generate_interview_metrics(frequency_score, rank):
    """Generate realistic interview metrics based on frequency."""
    # Estimate interview count (frequency_score / 5, with some variation)
    base_interviews = max(1, int(frequency_score / 5))
    interview_count = base_interviews + random.randint(-5, 5)
    interview_count = max(1, interview_count)
    
    # Estimate unique interviewers (roughly 50% of interviews)
    unique_interviewers = max(1, int(interview_count * 0.5) + random.randint(-2, 2))
    unique_interviewers = max(1, min(unique_interviewers, interview_count))
    
    # Calculate percentile (reverse rank)
    percentile = round(101 - rank, 2)
    
    # Generate difficulty rating (1-5 scale, higher frequency problems tend to be more standard)
    if rank <= 20:
        avg_difficulty = round(random.uniform(2.0, 3.5), 1)
    elif rank <= 50:
        avg_difficulty = round(random.uniform(2.5, 4.0), 1)
    else:
        avg_difficulty = round(random.uniform(3.0, 4.5), 1)
    
    # Generate success rate (easier problems have higher success rates)
    if avg_difficulty <= 2.5:
        success_rate = round(random.uniform(70, 90), 1)
    elif avg_difficulty <= 3.5:
        success_rate = round(random.uniform(50, 75), 1)
    else:
        success_rate = round(random.uniform(30, 60), 1)
    
    # Generate solve time (harder problems take longer)
    if avg_difficulty <= 2.5:
        solve_time = random.randint(10, 20)
    elif avg_difficulty <= 3.5:
        solve_time = random.randint(20, 35)
    else:
        solve_time = random.randint(35, 50)
    
    return {
        'interview_count': interview_count,
        'unique_interviewers': unique_interviewers,
        'percentile': percentile,
        'avg_difficulty_rating': avg_difficulty,
        'success_rate': success_rate,
        'avg_solve_time_minutes': solve_time
    }

def execute_mysql_command(command):
    """Execute MySQL command via docker."""
    try:
        result = subprocess.run([
            'docker', 'exec', 'codetop-mysql', 'mysql', '-u', 'root', '-proot', 'codetop_fsrs', '-e', command
        ], capture_output=True, text=True, check=True)
        return True, result.stdout
    except subprocess.CalledProcessError as e:
        return False, e.stderr

def main():
    print("Generating frequency statistics for 100 problems...")
    
    # Clear existing data for problems 1-100
    clear_cmd = "DELETE FROM problem_frequency_stats WHERE problem_id BETWEEN 1 AND 100;"
    success, output = execute_mysql_command(clear_cmd)
    if success:
        print("✓ Cleared existing frequency stats")
    else:
        print(f"✗ Failed to clear data: {output}")
        return
    
    # Generate inserts in batches of 10
    batch_size = 10
    total_inserted = 0
    
    for i in range(0, len(frequency_data), batch_size):
        batch = frequency_data[i:i+batch_size]
        
        # Build batch insert statement
        values = []
        for problem_id, frequency_score in batch:
            rank = problem_id  # Rank is same as problem_id for our ordered data
            metrics = generate_interview_metrics(frequency_score, rank)
            
            # Random recent date
            days_ago = random.randint(1, 7)
            last_asked = f"DATE_SUB(CURDATE(), INTERVAL {days_ago} DAY)"
            
            value = f"""({problem_id}, {frequency_score}, {metrics['interview_count']}, 
                       {metrics['unique_interviewers']}, {last_asked}, {rank}, 
                       {metrics['percentile']}, 'GLOBAL', {metrics['avg_difficulty_rating']}, 
                       {metrics['success_rate']}, {metrics['avg_solve_time_minutes']})"""
            values.append(value)
        
        insert_cmd = f"""INSERT INTO problem_frequency_stats (
            problem_id, total_frequency_score, interview_count, unique_interviewers,
            last_asked_date, frequency_rank, percentile, stats_scope,
            avg_difficulty_rating, success_rate, avg_solve_time_minutes
        ) VALUES {', '.join(values)};"""
        
        success, output = execute_mysql_command(insert_cmd)
        if success:
            total_inserted += len(batch)
            print(f"✓ Inserted batch {i//batch_size + 1}: Problems {batch[0][0]}-{batch[-1][0]} ({total_inserted}/100)")
        else:
            print(f"✗ Failed to insert batch {i//batch_size + 1}: {output}")
            break
    
    # Verify insertion
    count_cmd = "SELECT COUNT(*) as total FROM problem_frequency_stats WHERE stats_scope = 'GLOBAL';"
    success, output = execute_mysql_command(count_cmd)
    if success:
        print(f"\n✓ Verification: {output.strip()}")
    
    # Show top 10 by frequency
    top_cmd = """SELECT pfs.frequency_rank, p.title, pfs.total_frequency_score, pfs.percentile 
                 FROM problem_frequency_stats pfs 
                 JOIN problems p ON pfs.problem_id = p.id 
                 WHERE pfs.stats_scope = 'GLOBAL' 
                 ORDER BY pfs.frequency_rank LIMIT 10;"""
    success, output = execute_mysql_command(top_cmd)
    if success:
        print(f"\n✓ Top 10 Problems by Frequency:")
        print(output)

if __name__ == "__main__":
    random.seed(42)  # For reproducible results
    main()