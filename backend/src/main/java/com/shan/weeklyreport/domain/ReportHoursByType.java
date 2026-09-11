package com.shan.weeklyreport.domain;

import com.shan.weeklyreport.common.TaskType;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Hours spent by task type child entity of Report (C2-T05).
 */
@Entity
@Table(name = "report_hours_by_type", uniqueConstraints = {
    @UniqueConstraint(name = "uq_hours_by_type", columnNames = {"report_id", "task_type"})
})
public class ReportHoursByType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal hours = BigDecimal.ZERO;

    public ReportHoursByType() {}

    public ReportHoursByType(Report report, TaskType taskType, BigDecimal hours) {
        this.report = report;
        this.taskType = taskType;
        this.hours = hours;
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

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }
}
