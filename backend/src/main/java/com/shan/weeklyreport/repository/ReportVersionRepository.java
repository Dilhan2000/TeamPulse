package com.shan.weeklyreport.repository;

import com.shan.weeklyreport.domain.ReportVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ReportVersion entities (C3-T03).
 */
@Repository
public interface ReportVersionRepository extends JpaRepository<ReportVersion, Long> {

    Optional<ReportVersion> findTopByReportIdOrderByVersionNumberDesc(Long reportId);

    List<ReportVersion> findByReportIdOrderByVersionNumberDesc(Long reportId);

    Optional<ReportVersion> findByIdAndReportId(Long id, Long reportId);
}
