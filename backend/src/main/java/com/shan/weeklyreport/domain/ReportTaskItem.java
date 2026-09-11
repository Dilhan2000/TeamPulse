package com.shan.weeklyreport.domain;

import com.shan.weeklyreport.common.TaskPriority;
import com.shan.weeklyreport.common.TaskProgressStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Task item child entity of Report (C2-T05).
 */
@Entity
@Table(name = "report_task_items")
public class ReportTaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private TaskPriority priority;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TINYINT)
    @Column(name = "planned_percent", nullable = false)
    private int plannedPercent = 0;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TINYINT)
    @Column(name = "actual_percent", nullable = false)
    private int actualPercent = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskProgressStatus status;

    @Column(name = "time_planned_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal timePlannedHours = BigDecimal.ZERO;

    @Column(name = "time_spent_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal timeSpentHours = BigDecimal.ZERO;

    @Column(name = "deliverable", length = 500)
    private String deliverable;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public ReportTaskItem() {}

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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public int getPlannedPercent() {
        return plannedPercent;
    }

    public void setPlannedPercent(int plannedPercent) {
        this.plannedPercent = plannedPercent;
    }

    public int getActualPercent() {
        return actualPercent;
    }

    public void setActualPercent(int actualPercent) {
        this.actualPercent = actualPercent;
    }

    public TaskProgressStatus getStatus() {
        return status;
    }

    public void setStatus(TaskProgressStatus status) {
        this.status = status;
    }

    public BigDecimal getTimePlannedHours() {
        return timePlannedHours;
    }

    public void setTimePlannedHours(BigDecimal timePlannedHours) {
        this.timePlannedHours = timePlannedHours;
    }

    public BigDecimal getTimeSpentHours() {
        return timeSpentHours;
    }

    public void setTimeSpentHours(BigDecimal timeSpentHours) {
        this.timeSpentHours = timeSpentHours;
    }

    public String getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(String deliverable) {
        this.deliverable = deliverable;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
