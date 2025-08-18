package com.codetop.controller;

import com.codetop.annotation.CurrentUserId;
import com.codetop.dto.SubmissionResponse;
import com.codetop.dto.SubmitReportRequest;
import com.codetop.entity.InterviewReport;
import com.codetop.service.InterviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<SubmissionResponse> submitReport(
            @CurrentUserId Long userId,
            @Valid @RequestBody SubmitReportRequest request) {
        log.info("Received interview report submission from user {}: company={}, department={}, position={}", 
                userId, request.getCompany(), request.getDepartment(), request.getPosition());
        
        try {
            InterviewReport report = interviewReportService.submitReport(userId, request);
            
            SubmissionResponse response = SubmissionResponse.builder()
                    .success(true)
                    .reportId(report.getId())
                    .message("面试爆料提交成功！感谢您的贡献。")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to submit interview report for user {}", userId, e);
            
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

  }