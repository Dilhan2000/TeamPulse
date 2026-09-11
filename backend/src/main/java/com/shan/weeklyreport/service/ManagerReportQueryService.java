package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.ReportVersion;
import com.shan.weeklyreport.dto.ReportResponse;
import com.shan.weeklyreport.dto.ReportSummaryResponse;
import com.shan.weeklyreport.dto.ReportVersionSummaryResponse;
import com.shan.weeklyreport.dto.ReviewResponse;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.ReportMapper;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.ReportReviewRepository;
import com.shan.weeklyreport.repository.ReportVersionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for manager report queries across team reports (C3-T09, C4-T01).
 * Accessible by MANAGER role, strictly scoped to TEAM_MEMBER authors.
 */
@Service
@Transactional(readOnly = true)
public class ManagerReportQueryService {

    private final ReportRepository reportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportReviewRepository reportReviewRepository;

    public ManagerReportQueryService(ReportRepository reportRepository,
                                     ReportVersionRepository reportVersionRepository,
                                     ReportReviewRepository reportReviewRepository) {
        this.reportRepository = reportRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.reportReviewRepository = reportReviewRepository;
    }

    public Page<ReportSummaryResponse> findForManager(ReportStatus status,
                                                      Long userId,
                                                      Long projectId,
                                                      LocalDate weekStartFrom,
                                                      LocalDate weekStartTo,
                                                      Pageable pageable) {
        Specification<Report> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // C4-T01: Strictly scoped to TEAM_MEMBER authors only
            predicates.add(cb.equal(root.get("user").get("role"), Role.TEAM_MEMBER));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (projectId != null) {
                predicates.add(cb.equal(root.get("project").get("id"), projectId));
            }
            if (weekStartFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weekStartDate"), weekStartFrom));
            }
            if (weekStartTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weekStartDate"), weekStartTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reportRepository.findAll(spec, pageable)
                .map(ReportMapper::toSummaryResponse);
    }

    public ReportResponse getByIdForManager(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        // C4-T01: Managers cannot fetch draft content or non-TEAM_MEMBER reports
        if (report.getUser().getRole() != Role.TEAM_MEMBER || report.getStatus() == ReportStatus.DRAFT) {
            throw new ResourceNotFoundException("Report not found");
        }

        ReviewResponse latestReview = reportReviewRepository
                .findTopByReportIdOrderByIdDesc(reportId)
                .map(ReportMapper::toReviewResponse)
                .orElse(null);

        return ReportMapper.toResponse(report, latestReview);
    }

    public List<ReportVersionSummaryResponse> getVersionsForManager(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("Report not found");
        }

        return reportVersionRepository.findByReportIdOrderByVersionNumberDesc(reportId)
                .stream()
                .map(ReportMapper::toVersionSummary)
                .toList();
    }

    public ReportResponse getVersionForManager(Long reportId, Long versionId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("Report not found");
        }

        ReportVersion version = reportVersionRepository.findByIdAndReportId(versionId, reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found"));

        return ReportMapper.deserializeSnapshot(version.getContentSnapshot(), report, version.getSubmittedAt());
    }

    public List<ReviewResponse> getReviewsForManager(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("Report not found");
        }

        return reportReviewRepository.findByReportIdOrderByIdDesc(reportId)
                .stream()
                .map(ReportMapper::toReviewResponse)
                .toList();
    }
}
