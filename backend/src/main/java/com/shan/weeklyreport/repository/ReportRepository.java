package com.shan.weeklyreport.repository;

import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Report entity (C2-T06, C4-T02).
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    Optional<Report> findByIdAndUserId(Long id, Long userId);

    Optional<Report> findByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    boolean existsByUserIdAndWeekStartDate(Long userId, LocalDate weekStartDate);

    Page<Report> findByUserId(Long userId, Pageable pageable);

    List<Report> findByWeekStartDateAndUserRole(LocalDate weekStartDate, Role role);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM Report r
        WHERE r.status = 'NEEDS_CORRECTION'
          AND r.user.role = 'TEAM_MEMBER'
    """)
    long countNeedsCorrectionReports();

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(b) FROM Report r JOIN r.blockers b
        WHERE b.resolved = false
          AND r.user.role = 'TEAM_MEMBER'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
    """)
    long countOpenBlockers();

    @org.springframework.data.jpa.repository.Query("""
        SELECT MAX(r.weekStartDate) FROM Report r
        WHERE r.user.role = 'TEAM_MEMBER'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
    """)
    java.util.Optional<LocalDate> findLatestSubmittedReportWeek();

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(t) FROM Report r JOIN r.taskItems t
        WHERE t.status = 'COMPLETED'
          AND r.user.role = 'TEAM_MEMBER'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
          AND r.weekStartDate = :weekStartDate
          AND (:userId IS NULL OR r.user.id = :userId)
    """)
    long countCompletedTasksByWeek(
            @org.springframework.data.repository.query.Param("weekStartDate") LocalDate weekStartDate,
            @org.springframework.data.repository.query.Param("userId") Long userId
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM Report r
        WHERE r.user.role = 'TEAM_MEMBER'
          AND r.weekStartDate >= :from
          AND r.weekStartDate <= :to
    """)
    List<Report> findTeamReportsInRange(
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT r.project.id, r.project.name, SUM(t.timeSpentHours)
        FROM Report r JOIN r.taskItems t
        WHERE r.user.role = 'TEAM_MEMBER'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
          AND r.weekStartDate >= :from
          AND r.weekStartDate <= :to
        GROUP BY r.project.id, r.project.name
        ORDER BY SUM(t.timeSpentHours) DESC
    """)
    List<Object[]> findWorkloadByProjectRaw(
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT h.taskType, SUM(h.hours)
        FROM Report r JOIN r.hoursByType h
        WHERE r.user.role = 'TEAM_MEMBER'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
          AND r.weekStartDate >= :from
          AND r.weekStartDate <= :to
        GROUP BY h.taskType
    """)
    List<Object[]> findTimeByTaskTypeRaw(
            @org.springframework.data.repository.query.Param("from") LocalDate from,
            @org.springframework.data.repository.query.Param("to") LocalDate to
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT r FROM Report r
        JOIN FETCH r.user u
        WHERE r.user.role = 'TEAM_MEMBER'
          AND r.submittedAt IS NOT NULL
        ORDER BY r.submittedAt DESC
    """)
    List<Report> findRecentSubmissions(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM Report r
        WHERE r.user.id = :userId
          AND r.submittedAt IS NOT NULL
    """)
    long countSubmittedByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM Report r
        WHERE r.user.id = :userId
          AND r.status = 'APPROVED'
    """)
    long countApprovedByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COALESCE(SUM(h.hours), 0) FROM Report r
        JOIN r.hoursByType h
        WHERE r.user.id = :userId
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
    """)
    java.math.BigDecimal sumHoursLoggedByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(t) FROM Report r
        JOIN r.taskItems t
        WHERE r.user.id = :userId
          AND t.status = 'COMPLETED'
          AND r.status IN ('SUBMITTED', 'NEEDS_CORRECTION', 'APPROVED')
    """)
    long countCompletedTasksByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
