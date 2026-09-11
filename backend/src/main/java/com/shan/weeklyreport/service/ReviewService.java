package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.ReviewAction;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import com.shan.weeklyreport.domain.ReportReview;
import com.shan.weeklyreport.domain.ReportVersion;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ReportResponse;
import com.shan.weeklyreport.exception.ConflictException;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.ReportMapper;
import com.shan.weeklyreport.repository.ReportRepository;
import com.shan.weeklyreport.repository.ReportReviewRepository;
import com.shan.weeklyreport.repository.ReportVersionRepository;
import com.shan.weeklyreport.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for manager review actions: approve and request changes (C3-T07, C3-T08, C4-T01).
 */
@Service
@Transactional
public class ReviewService {

    private final ReportRepository reportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportReviewRepository reportReviewRepository;
    private final UserRepository userRepository;

    public ReviewService(ReportRepository reportRepository,
                         ReportVersionRepository reportVersionRepository,
                         ReportReviewRepository reportReviewRepository,
                         UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.reportReviewRepository = reportReviewRepository;
        this.userRepository = userRepository;
    }

    /**
     * Approve a submitted report (C3-T07).
     */
    public ReportResponse approve(Long managerId, Long reportId, String comment) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("Report not found");
        }

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new ConflictException("Only submitted reports can be approved");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager user not found"));

        ReportVersion latestVersion = reportVersionRepository
                .findTopByReportIdOrderByVersionNumberDesc(reportId)
                .orElseThrow(() -> new ConflictException("No submitted version found for report"));

        ReportReview review = new ReportReview(
                report,
                latestVersion,
                manager,
                ReviewAction.APPROVED,
                comment != null && !comment.trim().isEmpty() ? comment.trim() : null
        );
        reportReviewRepository.save(review);

        report.setStatus(ReportStatus.APPROVED);
        Report saved = reportRepository.save(report);

        return ReportMapper.toResponse(saved, ReportMapper.toReviewResponse(review));
    }

    /**
     * Request changes on a submitted report (C3-T08).
     */
    public ReportResponse requestChanges(Long managerId, Long reportId, String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required when requesting changes");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getUser().getRole() != Role.TEAM_MEMBER) {
            throw new ResourceNotFoundException("Report not found");
        }

        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new ConflictException("Only submitted reports can have changes requested");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager user not found"));

        ReportVersion latestVersion = reportVersionRepository
                .findTopByReportIdOrderByVersionNumberDesc(reportId)
                .orElseThrow(() -> new ConflictException("No submitted version found for report"));

        ReportReview review = new ReportReview(
                report,
                latestVersion,
                manager,
                ReviewAction.CHANGES_REQUESTED,
                comment.trim()
        );
        reportReviewRepository.save(review);

        report.setStatus(ReportStatus.NEEDS_CORRECTION);
        Report saved = reportRepository.save(report);

        return ReportMapper.toResponse(saved, ReportMapper.toReviewResponse(review));
    }
}
