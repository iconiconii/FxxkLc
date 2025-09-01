-- Create departments and company_departments tables for filter functionality
-- This adds support for department-level filtering within companies

-- Create departments table
CREATE TABLE departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    description TEXT NULL COMMENT '部门描述',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志',
    PRIMARY KEY (id),
    INDEX idx_departments_name (name),
    INDEX idx_departments_is_active (is_active),
    INDEX idx_departments_deleted (deleted),
    INDEX idx_departments_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- Create company_departments relationship table
CREATE TABLE company_departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL COMMENT '公司ID',
    department_id BIGINT NOT NULL COMMENT '部门ID',
    priority_level INT NOT NULL DEFAULT 0 COMMENT '优先级',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_department (company_id, department_id),
    INDEX idx_company_departments_company_id (company_id),
    INDEX idx_company_departments_department_id (department_id),
    INDEX idx_company_departments_priority (priority_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司部门关联表';

-- Insert some sample departments
INSERT INTO departments (name, description, sort_order) VALUES
('技术部', '负责技术研发、架构设计等工作', 1),
('产品部', '负责产品规划、需求分析等工作', 2),
('算法部', '负责算法研究、机器学习等工作', 3),
('数据部', '负责数据分析、数据工程等工作', 4),
('前端部', '负责前端开发、用户体验等工作', 5),
('后端部', '负责后端开发、系统架构等工作', 6),
('测试部', '负责软件测试、质量保证等工作', 7),
('运维部', '负责系统运维、基础设施等工作', 8);

-- Associate departments with existing companies (assuming company IDs 1-10 exist)
INSERT INTO company_departments (company_id, department_id, priority_level) 
SELECT c.id, d.id, 1 
FROM companies c 
CROSS JOIN departments d 
WHERE c.id <= 10 AND d.id <= 8;