package com.shan.weeklyreport.repository;

import com.shan.weeklyreport.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Project entity (C2-T04, C5-T02).
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByActiveTrue();

    @Query("""
        SELECT p FROM Project p
        WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:active IS NULL OR p.active = :active)
    """)
    Page<Project> searchProjects(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
