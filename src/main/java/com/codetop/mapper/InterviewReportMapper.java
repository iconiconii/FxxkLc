package com.codetop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codetop.entity.InterviewReport;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper interface for InterviewReport entity operations using MyBatis-Plus.
 * 
 * @author CodeTop Team
 */
@Mapper
@Repository
public interface InterviewReportMapper extends BaseMapper<InterviewReport> {

    /**
     * Find reports by company name.
     */
    @Select("""
            SELECT * FROM interview_reports 
            WHERE company_name = #{companyName} 
            AND deleted = 0 
            ORDER BY created_at DESC
            """)
    List<InterviewReport> findByCompanyName(@Param("companyName") String companyName);

    /**
     * Find recent reports.
     */
    @Select("""
            SELECT * FROM interview_reports 
            WHERE deleted = 0 
            ORDER BY created_at DESC 
            LIMIT #{limit}
            """)
    List<InterviewReport> findRecentReports(@Param("limit") int limit);

    /**
     * Find reports by status.
     */
    @Select("""
            SELECT * FROM interview_reports 
            WHERE status = #{status} 
            AND deleted = 0 
            ORDER BY created_at DESC
            """)
    List<InterviewReport> findByStatus(@Param("status") String status);

    /**
     * Find reports pending verification.
     */
    @Select("""
            SELECT * FROM interview_reports 
            WHERE verification_level = 'UNVERIFIED' 
            AND status = 'PENDING' 
            AND deleted = 0 
            ORDER BY created_at DESC
            """)
    List<InterviewReport> findPendingVerification();

    /**
     * Count reports by company.
     */
    @Select("""
            SELECT COUNT(*) FROM interview_reports 
            WHERE company_name = #{companyName} 
            AND deleted = 0
            """)
    Long countByCompany(@Param("companyName") String companyName);

    /**
     * Find top companies by report count.
     */
    @Select("""
            SELECT company_name, COUNT(*) as report_count 
            FROM interview_reports 
            WHERE deleted = 0 
            GROUP BY company_name 
            ORDER BY report_count DESC 
            LIMIT #{limit}
            """)
    @Results({
        @Result(column = "company_name", property = "companyName"),
        @Result(column = "report_count", property = "reportCount")
    })
    List<CompanyReportCount> getTopCompaniesByReportCount(@Param("limit") int limit);

    /**
     * Helper class for company report count results.
     */
    class CompanyReportCount {
        private String companyName;
        private Long reportCount;
        
        // Getters and setters
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public Long getReportCount() { return reportCount; }
        public void setReportCount(Long reportCount) { this.reportCount = reportCount; }
    }
}