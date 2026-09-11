package com.shan.weeklyreport.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity for project team member assignment (C5-B02).
 */
@Entity
@Table(name = "project_team_members")
public class ProjectTeamMember {

    @EmbeddedId
    private ProjectTeamMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    public ProjectTeamMember() {}

    public ProjectTeamMember(Project project, User user) {
        this.project = project;
        this.user = user;
        this.id = new ProjectTeamMemberId(project.getId(), user.getId());
        this.assignedAt = LocalDateTime.now();
    }

    public ProjectTeamMemberId getId() {
        return id;
    }

    public void setId(ProjectTeamMemberId id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
