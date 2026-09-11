package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for manager project management (C5-T07, C5-B03).
 */
@RestController
@RequestMapping("/api/manager/projects")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerProjectController {

    private final ProjectService projectService;

    public ManagerProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Paginated list of projects with optional name search and active status filter (C5-T07).
     */
    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> listProjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 50, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ProjectResponse> page = projectService.list(search, active, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Create a new project (C5-T07).
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest req) {
        ProjectResponse response = projectService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update project details (C5-T07).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest req) {
        ProjectResponse response = projectService.update(id, req);
        return ResponseEntity.ok(response);
    }

    /**
     * Toggle active status of a project (C5-T07).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProjectResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectStatusRequest req) {
        ProjectResponse response = projectService.updateStatus(id, req.active());
        return ResponseEntity.ok(response);
    }

    /**
     * Get assigned team members for a project (C5-B03).
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMemberOptionResponse>> getMembers(@PathVariable Long id) {
        List<TeamMemberOptionResponse> members = projectService.getMembers(id);
        return ResponseEntity.ok(members);
    }

    /**
     * Assign a team member to a project (C5-B03).
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> assignMember(
            @PathVariable Long id,
            @Valid @RequestBody AssignProjectMemberRequest req) {
        projectService.assignMember(id, req.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Remove an assigned team member from a project (C5-B03).
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
