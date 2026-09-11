package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.security.UserPrincipal;
import com.shan.weeklyreport.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for personal weekly reports (C2-T11, C2-T12, C3-T12).
 * Strictly accessible by TEAM_MEMBER and MANAGER only. ADMIN is excluded.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('TEAM_MEMBER','MANAGER')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse response = reportService.createDraft(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportResponse> updateReport(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse response = reportService.updateDraft(principal.id(), id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ReportResponse> submitReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse response = reportService.submit(principal.id(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse response = reportService.getReport(principal.id(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ReportSummaryResponse>> getMyReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartTo,
            @PageableDefault(size = 10, sort = "weekStartDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<ReportSummaryResponse> reports = reportService.getMyReports(
                principal.id(), status, weekStartFrom, weekStartTo, pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ReportVersionSummaryResponse>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ReportVersionSummaryResponse> versions = reportService.getVersions(principal.id(), id);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ReportResponse> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse versionSnapshot = reportService.getVersion(principal.id(), id, versionId);
        return ResponseEntity.ok(versionSnapshot);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ReviewResponse> reviews = reportService.getReviews(principal.id(), id);
        return ResponseEntity.ok(reviews);
    }
}
