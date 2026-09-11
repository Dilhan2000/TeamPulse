package com.shan.weeklyreport.controller;

import com.shan.weeklyreport.dto.ProjectSummaryResponse;
import com.shan.weeklyreport.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for project queries (C2-T13).
 */
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("isAuthenticated()")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getProjects(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<ProjectSummaryResponse> projects = projectService.getProjects(activeOnly);
        return ResponseEntity.ok(projects);
    }
}
