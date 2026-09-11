package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.Project;
import com.shan.weeklyreport.domain.ProjectTeamMember;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.*;
import com.shan.weeklyreport.exception.BadRequestException;
import com.shan.weeklyreport.exception.ConflictException;
import com.shan.weeklyreport.exception.DuplicateProjectNameException;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.ReportMapper;
import com.shan.weeklyreport.repository.ProjectRepository;
import com.shan.weeklyreport.repository.ProjectTeamMemberRepository;
import com.shan.weeklyreport.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for project queries and management (C2-T04, C2-T13, C5-T03 to C5-T06, C5-B02).
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectTeamMemberRepository projectTeamMemberRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectTeamMemberRepository projectTeamMemberRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.projectTeamMemberRepository = projectTeamMemberRepository;
        this.userRepository = userRepository;
    }

    /**
     * Public/Team dropdown list of projects (C2-T13).
     */
    public List<ProjectSummaryResponse> getProjects(boolean activeOnly) {
        List<Project> projects = activeOnly
                ? projectRepository.findByActiveTrue()
                : projectRepository.findAll();

        return projects.stream()
                .map(ReportMapper::toProjectSummary)
                .toList();
    }

    /**
     * Manager-facing paginated list with optional name search and active status filter (C5-T06).
     */
    public Page<ProjectResponse> list(String search, Boolean active, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return projectRepository.searchProjects(cleanSearch, active, pageable)
                .map(this::toResponse);
    }

    /**
     * Create project (C5-T03).
     */
    @Transactional
    public ProjectResponse create(CreateProjectRequest req) {
        String name = req.name().trim();
        String description = (req.description() != null && !req.description().isBlank()) ? req.description().trim() : null;

        Project project = new Project(name, description, true);
        try {
            Project saved = projectRepository.saveAndFlush(project);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProjectNameException("A project with the name '" + name + "' already exists");
        }
    }

    /**
     * Update project name and description (C5-T04).
     */
    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest req) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        String name = req.name().trim();
        String description = (req.description() != null && !req.description().isBlank()) ? req.description().trim() : null;

        project.setName(name);
        project.setDescription(description);

        try {
            Project updated = projectRepository.saveAndFlush(project);
            return toResponse(updated);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateProjectNameException("A project with the name '" + name + "' already exists");
        }
    }

    /**
     * Soft toggle project active status (C5-T05).
     */
    @Transactional
    public ProjectResponse updateStatus(Long id, boolean active) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        project.setActive(active);
        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    /**
     * List assigned team members for a project (C5-B02).
     */
    public List<TeamMemberOptionResponse> getMembers(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return projectTeamMemberRepository.findByProjectId(projectId).stream()
                .map(ptm -> new TeamMemberOptionResponse(ptm.getUser().getId(), ptm.getUser().getFullName()))
                .toList();
    }

    /**
     * Assign an active team member to a project (C5-B02).
     */
    @Transactional
    public void assignMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.TEAM_MEMBER || user.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Only active TEAM_MEMBER users can be assigned to a project");
        }

        if (projectTeamMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ConflictException("User is already assigned to this project");
        }

        ProjectTeamMember assignment = new ProjectTeamMember(project, user);
        projectTeamMemberRepository.save(assignment);
    }

    /**
     * Remove an assigned team member from a project (C5-B02).
     */
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        ProjectTeamMember assignment = projectTeamMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User is not assigned to this project"));

        projectTeamMemberRepository.delete(assignment);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.isActive(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
