package com.shan.weeklyreport.repository;

import com.shan.weeklyreport.domain.ProjectTeamMember;
import com.shan.weeklyreport.domain.ProjectTeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for project team member assignments (C5-B02).
 */
@Repository
public interface ProjectTeamMemberRepository extends JpaRepository<ProjectTeamMember, ProjectTeamMemberId> {

    List<ProjectTeamMember> findByProjectId(Long projectId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectTeamMember> findByProjectIdAndUserId(Long projectId, Long userId);

    void deleteByProjectIdAndUserId(Long projectId, Long userId);
}
