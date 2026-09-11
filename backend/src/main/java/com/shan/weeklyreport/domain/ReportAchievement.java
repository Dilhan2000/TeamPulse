package com.shan.weeklyreport.domain;

import jakarta.persistence.*;

/**
 * Achievement child entity of Report (C2-T05).
 */
@Entity
@Table(name = "report_achievements")
public class ReportAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "is_key_achievement", nullable = false)
    private boolean isKeyAchievement = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public ReportAchievement() {}

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isKeyAchievement() {
        return isKeyAchievement;
    }

    public void setKeyAchievement(boolean keyAchievement) {
        isKeyAchievement = keyAchievement;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
