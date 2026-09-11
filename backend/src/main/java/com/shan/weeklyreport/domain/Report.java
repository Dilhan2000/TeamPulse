package com.shan.weeklyreport.domain;

import com.shan.weeklyreport.common.ReportStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Weekly Report domain entity (C2-T05).
 */
@Entity
@Table(name = "reports", uniqueConstraints = {
    @UniqueConstraint(name = "uq_reports_user_week", columnNames = {"user_id", "week_start_date"})
})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportTaskItem> taskItems = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportNextWeekTask> nextWeekTasks = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportBlocker> blockers = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReportAchievement> achievements = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportHoursByType> hoursByType = new ArrayList<>();

    public Report() {}

    public Report(User user, Project project, LocalDate weekStartDate, LocalDate weekEndDate) {
        this.user = user;
        this.project = project;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.status = ReportStatus.DRAFT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ReportTaskItem> getTaskItems() {
        return taskItems;
    }

    public void setTaskItems(List<ReportTaskItem> taskItems) {
        this.taskItems = taskItems;
    }

    public List<ReportNextWeekTask> getNextWeekTasks() {
        return nextWeekTasks;
    }

    public void setNextWeekTasks(List<ReportNextWeekTask> nextWeekTasks) {
        this.nextWeekTasks = nextWeekTasks;
    }

    public List<ReportBlocker> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<ReportBlocker> blockers) {
        this.blockers = blockers;
    }

    public List<ReportAchievement> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<ReportAchievement> achievements) {
        this.achievements = achievements;
    }

    public List<ReportHoursByType> getHoursByType() {
        return hoursByType;
    }

    public void setHoursByType(List<ReportHoursByType> hoursByType) {
        this.hoursByType = hoursByType;
    }
}
