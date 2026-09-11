package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.security.UserPrincipal;
import com.shan.weeklyreport.service.ManagerReportQueryService;
import com.shan.weeklyreport.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for manager report listing, review actions, and version queries (C3-T10, C3-T11, C3-T12).
 * Strictly restricted to MANAGER role.
 */
@RestController
@RequestMapping("/api/manager/reports")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerReportController {

    private final ManagerReportQueryService managerQueryService;
    private final ReviewService reviewService;

    public ManagerReportController(ManagerReportQueryService managerQueryService,
                                   ReviewService reviewService) {
        this.managerQueryService = managerQueryService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<Page<ReportSummaryResponse>> listReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartTo,
            @PageableDefault(size = 10, sort = "weekStartDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReportSummaryResponse> page = managerQueryService.findForManager(
                status, userId, projectId, weekStartFrom, weekStartTo, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReport(@PathVariable Long id) {
        ReportResponse report = managerQueryService.getByIdForManager(id);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReportResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApproveReportRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String comment = request != null ? request.comment() : null;
        ReportResponse response = reviewService.approve(principal.id(), id, comment);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/request-changes")
    public ResponseEntity<ReportResponse> requestChanges(
            @PathVariable Long id,
            @Valid @RequestBody RequestChangesRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ReportResponse response = reviewService.requestChanges(principal.id(), id, request.comment());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ReportVersionSummaryResponse>> getVersions(@PathVariable Long id) {
        List<ReportVersionSummaryResponse> versions = managerQueryService.getVersionsForManager(id);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}/versions/{versionId}")
    public ResponseEntity<ReportResponse> getVersion(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        ReportResponse versionSnapshot = managerQueryService.getVersionForManager(id, versionId);
        return ResponseEntity.ok(versionSnapshot);
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long id) {
        List<ReviewResponse> reviews = managerQueryService.getReviewsForManager(id);
        return ResponseEntity.ok(reviews);
    }
}
