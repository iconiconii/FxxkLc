-- V4: CodeTop Style Filtering Enhancement - Organization Structure
-- Add standardized departments and positions tables for CodeTop-style filtering

-- ===================================
-- 1. Departments Table
-- ===================================
CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Department ID',
    name VARCHAR(100) NOT NULL COMMENT 'Department name (e.g., "后端开发", "前端开发", "算法工程师")',
    display_name VARCHAR(100) COMMENT 'Display name for UI',
    description TEXT COMMENT 'Department description',
    type ENUM('ENGINEERING', 'PRODUCT', 'DATA', 'BUSINESS', 'OTHER') DEFAULT 'ENGINEERING' COMMENT 'Department type',
    
    -- Hierarchy support
    parent_department_id BIGINT COMMENT 'Parent department ID for nested structure',
    level TINYINT DEFAULT 1 COMMENT 'Department level in hierarchy (1=top level)',
    sort_order INT DEFAULT 0 COMMENT 'Sort order for display',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Department active status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    UNIQUE KEY uk_name (name),
    INDEX idx_parent (parent_department_id),
    INDEX idx_type (type),
    INDEX idx_level (level),
    INDEX idx_active (is_active, deleted),
    INDEX idx_sort (sort_order)
) ENGINE=InnoDB COMMENT 'Standardized departments table for CodeTop-style filtering';

-- ===================================
-- 2. Positions Table  
-- ===================================
CREATE TABLE positions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Position ID',
    name VARCHAR(100) NOT NULL COMMENT 'Position name (e.g., "Java工程师", "前端工程师", "算法工程师")',
    display_name VARCHAR(100) COMMENT 'Display name for UI',
    description TEXT COMMENT 'Position description',
    level ENUM('INTERN', 'JUNIOR', 'MIDDLE', 'SENIOR', 'EXPERT', 'PRINCIPAL') DEFAULT 'MIDDLE' COMMENT 'Position level',
    type ENUM('ENGINEERING', 'PRODUCT', 'DATA', 'BUSINESS', 'OTHER') DEFAULT 'ENGINEERING' COMMENT 'Position type',
    
    -- Requirements
    skills_required JSON COMMENT 'Required skills in JSON format',
    experience_years_min INT DEFAULT 0 COMMENT 'Minimum years of experience required',
    experience_years_max INT DEFAULT 10 COMMENT 'Maximum years of experience',
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Position active status',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Soft Delete
    deleted TINYINT DEFAULT 0 COMMENT 'Logical delete flag',
    
    -- Indexes
    UNIQUE KEY uk_name (name),
    INDEX idx_level (level),
    INDEX idx_type (type),
    INDEX idx_experience (experience_years_min, experience_years_max),
    INDEX idx_active (is_active, deleted)
) ENGINE=InnoDB COMMENT 'Standardized positions table for CodeTop-style filtering';

-- ===================================
-- 3. Company Departments Association Table
-- ===================================
CREATE TABLE company_departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Association ID',
    company_id BIGINT NOT NULL COMMENT 'Company ID (logical reference to companies.id)',
    department_id BIGINT NOT NULL COMMENT 'Department ID (logical reference to departments.id)',
    
    -- Association details
    department_size ENUM('SMALL', 'MEDIUM', 'LARGE', 'VERY_LARGE') COMMENT 'Department size in this company',
    hiring_status ENUM('ACTIVELY_HIRING', 'OCCASIONAL', 'NOT_HIRING') DEFAULT 'OCCASIONAL' COMMENT 'Current hiring status',
    priority_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM' COMMENT 'Department priority in company',
    
    -- Custom naming for this company
    custom_name VARCHAR(100) COMMENT 'Company-specific department name if different from standard',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Constraints
    UNIQUE KEY uk_company_department (company_id, department_id),
    
    -- Indexes
    INDEX idx_company_id (company_id),
    INDEX idx_department_id (department_id),
    INDEX idx_hiring_status (hiring_status),
    INDEX idx_priority (priority_level)
) ENGINE=InnoDB COMMENT 'Company-Department association table';

-- ===================================
-- 4. Department Positions Association Table
-- ===================================
CREATE TABLE department_positions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Association ID',
    department_id BIGINT NOT NULL COMMENT 'Department ID (logical reference to departments.id)',
    position_id BIGINT NOT NULL COMMENT 'Position ID (logical reference to positions.id)',
    
    -- Association details
    is_primary BOOLEAN DEFAULT FALSE COMMENT 'Whether this is a primary position for the department',
    demand_level ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM' COMMENT 'Demand level for this position',
    interview_frequency ENUM('RARE', 'OCCASIONAL', 'FREQUENT', 'VERY_FREQUENT') DEFAULT 'OCCASIONAL' COMMENT 'How often this position is interviewed',
    
    -- Custom requirements for this department
    custom_requirements JSON COMMENT 'Department-specific requirements in JSON format',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
    
    -- Constraints
    UNIQUE KEY uk_department_position (department_id, position_id),
    
    -- Indexes
    INDEX idx_department_id (department_id),
    INDEX idx_position_id (position_id),
    INDEX idx_is_primary (is_primary),
    INDEX idx_demand_level (demand_level),
    INDEX idx_interview_frequency (interview_frequency)
) ENGINE=InnoDB COMMENT 'Department-Position association table';

-- ===================================
-- 5. Insert Sample Data
-- ===================================

-- Insert sample departments
INSERT INTO departments (name, display_name, description, type, level) VALUES
('后端开发', '后端开发部', '负责服务端开发、API设计、数据库设计等', 'ENGINEERING', 1),
('前端开发', '前端开发部', '负责Web前端、移动端UI开发等', 'ENGINEERING', 1),
('算法工程师', '算法工程部', '负责机器学习、深度学习、推荐算法等', 'ENGINEERING', 1),
('数据开发', '数据开发部', '负责数据仓库、ETL、数据分析等', 'DATA', 1),
('测试开发', '测试开发部', '负责自动化测试、测试平台开发等', 'ENGINEERING', 1),
('运维开发', '运维开发部', '负责DevOps、基础设施、监控等', 'ENGINEERING', 1),
('产品经理', '产品部', '负责产品规划、需求分析、用户体验等', 'PRODUCT', 1),
('安全工程师', '安全部', '负责信息安全、安全开发、风控等', 'ENGINEERING', 1);

-- Insert sample positions
INSERT INTO positions (name, display_name, description, level, type, experience_years_min, experience_years_max) VALUES
('Java工程师', 'Java开发工程师', '使用Java技术栈进行后端开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('Python工程师', 'Python开发工程师', '使用Python进行后端开发或数据开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('Go工程师', 'Go开发工程师', '使用Go语言进行后端服务开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('React工程师', 'React前端工程师', '使用React技术栈进行前端开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('Vue工程师', 'Vue前端工程师', '使用Vue技术栈进行前端开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('算法工程师', '算法工程师', '负责机器学习算法设计与实现', 'MIDDLE', 'ENGINEERING', 2, 8),
('数据工程师', '数据工程师', '负责数据处理、ETL、数据仓库建设', 'MIDDLE', 'DATA', 1, 6),
('测试工程师', '测试工程师', '负责软件测试、自动化测试开发', 'MIDDLE', 'ENGINEERING', 1, 5),
('DevOps工程师', 'DevOps工程师', '负责CI/CD、基础设施、运维自动化', 'MIDDLE', 'ENGINEERING', 2, 6),
('产品经理', '产品经理', '负责产品规划和需求管理', 'MIDDLE', 'PRODUCT', 2, 8),
('安全工程师', '安全工程师', '负责信息安全和安全开发', 'MIDDLE', 'ENGINEERING', 2, 6);

-- Create associations between companies and departments (sample for major tech companies)
INSERT INTO company_departments (company_id, department_id, hiring_status, priority_level)
SELECT c.id, d.id, 'ACTIVELY_HIRING', 'HIGH'
FROM companies c
CROSS JOIN departments d
WHERE c.name IN ('google', 'apple', 'microsoft', 'amazon', 'facebook')
  AND d.name IN ('后端开发', '前端开发', '算法工程师', '数据开发')
  AND c.deleted = 0 AND d.deleted = 0;

-- Create associations between departments and positions
INSERT INTO department_positions (department_id, position_id, is_primary, demand_level, interview_frequency)
VALUES
-- 后端开发部门的核心岗位
((SELECT id FROM departments WHERE name = '后端开发'), (SELECT id FROM positions WHERE name = 'Java工程师'), TRUE, 'HIGH', 'VERY_FREQUENT'),
((SELECT id FROM departments WHERE name = '后端开发'), (SELECT id FROM positions WHERE name = 'Python工程师'), TRUE, 'HIGH', 'FREQUENT'),
((SELECT id FROM departments WHERE name = '后端开发'), (SELECT id FROM positions WHERE name = 'Go工程师'), TRUE, 'MEDIUM', 'FREQUENT'),
-- 前端开发部门的核心岗位
((SELECT id FROM departments WHERE name = '前端开发'), (SELECT id FROM positions WHERE name = 'React工程师'), TRUE, 'HIGH', 'VERY_FREQUENT'),
((SELECT id FROM departments WHERE name = '前端开发'), (SELECT id FROM positions WHERE name = 'Vue工程师'), TRUE, 'HIGH', 'FREQUENT'),
-- 算法工程师部门的核心岗位
((SELECT id FROM departments WHERE name = '算法工程师'), (SELECT id FROM positions WHERE name = '算法工程师'), TRUE, 'HIGH', 'FREQUENT'),
-- 数据开发部门的核心岗位
((SELECT id FROM departments WHERE name = '数据开发'), (SELECT id FROM positions WHERE name = '数据工程师'), TRUE, 'HIGH', 'FREQUENT'),
((SELECT id FROM departments WHERE name = '数据开发'), (SELECT id FROM positions WHERE name = 'Python工程师'), FALSE, 'MEDIUM', 'OCCASIONAL'),
-- 测试开发部门的核心岗位
((SELECT id FROM departments WHERE name = '测试开发'), (SELECT id FROM positions WHERE name = '测试工程师'), TRUE, 'MEDIUM', 'OCCASIONAL'),
-- 运维开发部门的核心岗位
((SELECT id FROM departments WHERE name = '运维开发'), (SELECT id FROM positions WHERE name = 'DevOps工程师'), TRUE, 'MEDIUM', 'OCCASIONAL'),
-- 产品部门的核心岗位
((SELECT id FROM departments WHERE name = '产品经理'), (SELECT id FROM positions WHERE name = '产品经理'), TRUE, 'MEDIUM', 'OCCASIONAL'),
-- 安全部门的核心岗位
((SELECT id FROM departments WHERE name = '安全工程师'), (SELECT id FROM positions WHERE name = '安全工程师'), TRUE, 'MEDIUM', 'OCCASIONAL');