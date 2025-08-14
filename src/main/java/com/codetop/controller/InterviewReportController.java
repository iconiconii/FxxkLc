package com.codetop.controller;

import com.codetop.entity.InterviewReport;
import com.codetop.service.InterviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * Interview report controller for community-driven interview data collection.
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interview Reports", description = "Community interview data collection")
public class InterviewReportController {

    private final InterviewReportService interviewReportService;

    /**
     * Submit a new interview report.
     */
    @PostMapping
    @Operation(summary = "Submit interview report", description = "Submit a new interview report from community")
    public ResponseEntity<SubmissionResponse> submitReport(@Valid @RequestBody SubmitReportRequest request) {
        log.info("Received interview report submission: company={}, department={}, position={}", 
                request.getCompany(), request.getDepartment(), request.getPosition());
        
        try {
            InterviewReport report = interviewReportService.submitReport(request);
            
            SubmissionResponse response = SubmissionResponse.builder()
                    .success(true)
                    .reportId(report.getId())
                    .message("面试爆料提交成功！感谢您的贡献。")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to submit interview report", e);
            
            SubmissionResponse response = SubmissionResponse.builder()
                    .success(false)
                    .message("提交失败，请稍后重试。")
                    .build();
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get interview reports by company.
     */
    @GetMapping("/company/{companyName}")
    @Operation(summary = "Get reports by company", description = "Get interview reports for a specific company")
    public ResponseEntity<List<InterviewReport>> getReportsByCompany(@PathVariable String companyName) {
        List<InterviewReport> reports = interviewReportService.getReportsByCompany(companyName);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get recent interview reports.
     */
    @GetMapping("/recent")
    @Operation(summary = "Get recent reports", description = "Get recent interview reports")
    public ResponseEntity<List<InterviewReport>> getRecentReports(
            @RequestParam(defaultValue = "20") int limit) {
        List<InterviewReport> reports = interviewReportService.getRecentReports(limit);
        return ResponseEntity.ok(reports);
    }

    // DTOs

    @lombok.Data
    @lombok.Builder
    public static class SubmitReportRequest {
        private String company;
        private String department;
        private String position;
        private String problemSearch;
        private String date;
        private String additionalNotes;
        private Integer difficultyRating;
        private InterviewReport.InterviewRound interviewRound;
    }

    @lombok.Data
    @lombok.Builder
    public static class SubmissionResponse {
        private boolean success;
        private Long reportId;
        private String message;
        private String errorCode;
    }
}