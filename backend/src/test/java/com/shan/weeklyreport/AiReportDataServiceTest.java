package com.shan.weeklyreport;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.ReportStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.common.TaskProgressStatus;
import com.shan.weeklyreport.domain.*;
import com.shan.weeklyreport.dto.ai.ReportSummaryForAi;
import com.shan.weeklyreport.repository.*;
import com.shan.weeklyreport.service.ai.AiReportDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AiReportDataServiceTest {

    @Autowired
    private AiReportDataService aiReportDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReportReviewRepository reportReviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User teamMember1;
    private User teamMember2;
    private User managerUser;
    private Project projectAlpha;
    private Project projectBeta;

    @BeforeEach
    void setUp() {
        reportReviewRepository.deleteAll();
        reportVersionRepository.deleteAll();
        reportRepository.deleteAll();
        projectRepository.deleteAll();

        userRepository.findAll().forEach(u -> {
            if (!"admin@weeklyreport.local".equals(u.getEmail())) {
                userRepository.delete(u);
            }
        });

        if (userRepository.findByEmail("admin@weeklyreport.local").isEmpty()) {
            User admin = new User(
                    "Admin System",
                    "admin@weeklyreport.local",
                    passwordEncoder.encode("ChangeMe123!"),
                    Role.ADMIN,
                    AccountStatus.ACTIVE
            );
            userRepository.save(admin);
        }

        teamMember1 = new User("Alice Engineer", "alice@example.com", passwordEncoder.encode("Password123!"), Role.TEAM_MEMBER, AccountStatus.ACTIVE);
        teamMember1 = userRepository.save(teamMember1);

        teamMember2 = new User("Bob Developer", "bob@example.com", passwordEncoder.encode("Password123!"), Role.TEAM_MEMBER, AccountStatus.ACTIVE);
        teamMember2 = userRepository.save(teamMember2);

        managerUser = new User("Carol Manager", "carol@example.com", passwordEncoder.encode("Password123!"), Role.MANAGER, AccountStatus.ACTIVE);
        managerUser = userRepository.save(managerUser);

        projectAlpha = new Project("Project Alpha", "Description Alpha", true);
        projectAlpha = projectRepository.save(projectAlpha);

        projectBeta = new Project("Project Beta", "Description Beta", true);
        projectBeta = projectRepository.save(projectBeta);
    }

    @Test
    @DisplayName("C8-T02: DRAFT reports are completely excluded even when matching all query filters")
    void shouldExcludeDraftReportsEntirely() {
        LocalDate week = LocalDate.of(2026, 3, 2);

        // Submitted report for Alice
        Report submitted = new Report(teamMember1, projectAlpha, week, week.plusDays(6));
        submitted.setStatus(ReportStatus.SUBMITTED);
        reportRepository.save(submitted);

        // Draft report for Bob
        Report draft = new Report(teamMember2, projectAlpha, week, week.plusDays(6));
        draft.setStatus(ReportStatus.DRAFT);
        reportRepository.save(draft);

        List<ReportSummaryForAi> results = aiReportDataService.queryReports(week, null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).memberName()).isEqualTo("Alice Engineer");
        assertThat(results.get(0).status()).isEqualTo(ReportStatus.SUBMITTED);
    }

    @Test
    @DisplayName("C8-T02: Manager's own report is excluded from AI report data")
    void shouldExcludeManagerAuthoredReports() {
        LocalDate week = LocalDate.of(2026, 3, 2);

        // Member report
        Report memberReport = new Report(teamMember1, projectAlpha, week, week.plusDays(6));
        memberReport.setStatus(ReportStatus.APPROVED);
        reportRepository.save(memberReport);

        // Manager's own report (even if approved/submitted)
        Report managerReport = new Report(managerUser, projectAlpha, week, week.plusDays(6));
        managerReport.setStatus(ReportStatus.APPROVED);
        reportRepository.save(managerReport);

        List<ReportSummaryForAi> results = aiReportDataService.queryReports(week, null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).memberName()).isEqualTo("Alice Engineer");
    }

    @Test
    @Transactional
    @DisplayName("C8-T02: Successfully filters by project, member, and date range")
    void shouldFilterByProjectAndDateRange() {
        LocalDate week1 = LocalDate.of(2026, 3, 2);
        LocalDate week2 = LocalDate.of(2026, 3, 9);
        LocalDate week3 = LocalDate.of(2026, 3, 16);

        Report r1 = new Report(teamMember1, projectAlpha, week1, week1.plusDays(6));
        r1.setStatus(ReportStatus.APPROVED);
        reportRepository.save(r1);

        Report r2 = new Report(teamMember1, projectBeta, week2, week2.plusDays(6));
        r2.setStatus(ReportStatus.SUBMITTED);
        reportRepository.save(r2);

        Report r3 = new Report(teamMember2, projectAlpha, week3, week3.plusDays(6));
        r3.setStatus(ReportStatus.NEEDS_CORRECTION);
        reportRepository.save(r3);

        // Filter by projectAlpha only
        List<ReportSummaryForAi> alphaResults = aiReportDataService.queryReports(null, null, null, projectAlpha.getId(), null);
        assertThat(alphaResults).hasSize(2);
        assertThat(alphaResults).extracting(ReportSummaryForAi::projectName).containsOnly("Project Alpha");

        // Filter by date range week1 to week2
        List<ReportSummaryForAi> rangeResults = aiReportDataService.queryReports(null, week1, week2, null, null);
        assertThat(rangeResults).hasSize(2);

        // Filter by specific user
        List<ReportSummaryForAi> userResults = aiReportDataService.queryReports(null, null, null, null, teamMember2.getId());
        assertThat(userResults).hasSize(1);
        assertThat(userResults.get(0).memberName()).isEqualTo("Bob Developer");
    }

    @Test
    @DisplayName("C8-T02: Unfiltered query respects maximum 50-result defensive cap")
    void shouldRespectMaxResultsCap() {
        LocalDate baseWeek = LocalDate.of(2025, 1, 6);
        // Create 55 reports across 55 weeks for teamMember1
        for (int i = 0; i < 55; i++) {
            LocalDate week = baseWeek.plusWeeks(i);
            Report r = new Report(teamMember1, projectAlpha, week, week.plusDays(6));
            r.setStatus(ReportStatus.SUBMITTED);
            reportRepository.save(r);
        }

        List<ReportSummaryForAi> results = aiReportDataService.queryReports(null, null, null, null, null);
        assertThat(results).hasSize(50);
    }
}
