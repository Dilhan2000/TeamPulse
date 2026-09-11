package com.shan.weeklyreport.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Historical snapshot of a submitted weekly report (C3-T01, C3-T03).
 */
@Entity
@Table(name = "report_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_report_versions_number", columnNames = {"report_id", "version_number"})
})
public class ReportVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_snapshot", nullable = false, columnDefinition = "json")
    private String contentSnapshot;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public ReportVersion() {}

    public ReportVersion(Report report, int versionNumber, String contentSnapshot, LocalDateTime submittedAt) {
        this.report = report;
        this.versionNumber = versionNumber;
        this.contentSnapshot = contentSnapshot;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getContentSnapshot() {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot) {
        this.contentSnapshot = contentSnapshot;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
