package com.codetop.service;

import com.codetop.controller.InterviewReportController;
import com.codetop.entity.InterviewReport;
import com.codetop.mapper.InterviewReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Service for managing interview reports.
 * 
 * @author CodeTop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewReportService {

    private final InterviewReportMapper interviewReportMapper;

    /**
     * Submit a new interview report.
     */
    public InterviewReport submitReport(InterviewReportController.SubmitReportRequest request) {
        log.info("Processing interview report submission for company: {}", request.getCompany());

        // Create new interview report
        InterviewReport report = new InterviewReport();
        report.setCompanyName(request.getCompany());
        report.setDepartment(request.getDepartment());
        report.setPosition(request.getPosition());
        report.setProblemTitle(request.getProblemSearch());
        report.setAdditionalNotes(request.getAdditionalNotes());
        report.setDifficultyRating(request.getDifficultyRating());
        report.setInterviewRound(request.getInterviewRound() != null ? 
                request.getInterviewRound() : InterviewReport.InterviewRound.OTHER);

        // Parse interview date
        if (request.getDate() != null && !request.getDate().isEmpty()) {
            try {
                LocalDate interviewDate = LocalDate.parse(request.getDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                report.setInterviewDate(interviewDate);
            } catch (DateTimeParseException e) {
                log.warn("Invalid date format: {}, using current date", request.getDate());
                report.setInterviewDate(LocalDate.now());
            }
        } else {
            report.setInterviewDate(LocalDate.now());
        }

        // Set default values
        report.setDataSource(InterviewReport.DataSource.USER_REPORT);
        report.setDataCollectionMethod(InterviewReport.DataCollectionMethod.FORM_SUBMISSION);
        report.setStatus(InterviewReport.ReportStatus.PENDING);
        report.setVerificationLevel(InterviewReport.VerificationLevel.UNVERIFIED);
        report.setIsVerified(false);
        report.setIsDuplicate(false);
        report.setDuplicateCheckStatus(InterviewReport.DuplicateCheckStatus.PENDING);
        report.setCredibilityScore(new BigDecimal("50.0")); // Default credibility
        report.setReportQualityScore(calculateQualityScore(report));
        report.setUpvoteCount(0);
        report.setDownvoteCount(0);
        report.setLanguageCode("zh-CN");
        report.setReportingRegion("CN");

        // Save to database
        interviewReportMapper.insert(report);
        
        log.info("Successfully submitted interview report with ID: {}", report.getId());
        return report;
    }

    /**
     * Get interview reports by company name.
     */
    public List<InterviewReport> getReportsByCompany(String companyName) {
        return interviewReportMapper.findByCompanyName(companyName);
    }

    /**
     * Get recent interview reports.
     */
    public List<InterviewReport> getRecentReports(int limit) {
        return interviewReportMapper.findRecentReports(limit);
    }

    /**
     * Calculate quality score based on report completeness.
     */
    private BigDecimal calculateQualityScore(InterviewReport report) {
        double score = 0.0;
        
        // Base score for required fields
        if (report.getCompanyName() != null && !report.getCompanyName().trim().isEmpty()) score += 20;
        if (report.getDepartment() != null && !report.getDepartment().trim().isEmpty()) score += 20;
        if (report.getPosition() != null && !report.getPosition().trim().isEmpty()) score += 20;
        if (report.getProblemTitle() != null && !report.getProblemTitle().trim().isEmpty()) score += 20;
        if (report.getInterviewDate() != null) score += 10;
        
        // Bonus for additional details
        if (report.getAdditionalNotes() != null && !report.getAdditionalNotes().trim().isEmpty()) score += 5;
        if (report.getDifficultyRating() != null && report.getDifficultyRating() > 0) score += 5;
        
        return new BigDecimal(score);
    }
}