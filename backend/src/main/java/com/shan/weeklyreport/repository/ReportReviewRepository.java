package com.shan.weeklyreport.repository;

import com.shan.weeklyreport.domain.ReportReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ReportReview entities (C3-T03).
 */
@Repository
public interface ReportReviewRepository extends JpaRepository<ReportReview, Long> {

    List<ReportReview> findByReportIdOrderByIdDesc(Long reportId);

    Optional<ReportReview> findTopByReportIdOrderByIdDesc(Long reportId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT rr FROM ReportReview rr
        JOIN FETCH rr.report r
        JOIN FETCH r.user u
        JOIN FETCH rr.reviewer rev
        WHERE r.user.role = 'TEAM_MEMBER'
        ORDER BY rr.reviewedAt DESC
    """)
    List<ReportReview> findRecentReviews(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(rr) FROM ReportReview rr
        WHERE rr.report.user.id = :userId
          AND rr.action = 'CHANGES_REQUESTED'
    """)
    long countChangesRequestedByReportUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
