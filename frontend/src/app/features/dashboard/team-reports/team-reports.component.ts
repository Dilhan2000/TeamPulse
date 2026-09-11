import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { ManagerReportService } from '../../../core/services/manager-report.service';
import { ProjectService } from '../../../core/services/project.service';
import {
  ManagerReportFilterParams,
  Project,
  ReportSummary,
  SectionRow,
  SectionType,
  TeamMemberOption,
  TeamWeekStatusRow,
} from '../../../core/models/report.model';

@Component({
  selector: 'app-team-reports',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTabsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatCardModule,
    MatChipsModule,
    MatButtonToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './team-reports.component.html',
  styleUrl: './team-reports.component.scss',
})
export class TeamReportsComponent implements OnInit {
  private readonly teamDashboardService = inject(TeamDashboardService);
  private readonly managerReportService = inject(ManagerReportService);
  private readonly projectService = inject(ProjectService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  activeTab = signal<number>(0);
  selectedSection = signal<SectionType>('BLOCKERS');
  needsMyReview = signal<boolean>(false);

  selectedWeekDate: Date = this.getNearestMonday(new Date());
  rangeFrom: Date | null = null;
  rangeTo: Date | null = null;
  selectedUserId: number | null = null;
  selectedProjectId: number | null = null;

  activeProjects = signal<Project[]>([]);
  teamMembers = signal<TeamMemberOption[]>([]);

  // Tab 1: Week status
  weekRows = signal<TeamWeekStatusRow[]>([]);
  isLoadingWeek = signal<boolean>(false);
  weekColumns: string[] = ['name', 'project', 'status', 'submittedAt', 'action'];

  // Tab 2: Range reports
  rangeReports = signal<ReportSummary[]>([]);
  totalRangeElements = signal<number>(0);
  rangePageIndex = signal<number>(0);
  rangePageSize = signal<number>(10);
  isLoadingRange = signal<boolean>(false);
  rangeColumns: string[] = ['member', 'project', 'week', 'submittedAt', 'status', 'actions'];

  // Tab 3: Section rows
  sectionRows = signal<SectionRow[]>([]);
  isLoadingSection = signal<boolean>(false);

  mondayDateFilter = (d: Date | null): boolean => {
    const day = (d || new Date()).getDay();
    return day === 1; // Monday only
  };

  ngOnInit(): void {
    this.loadFilterOptions();

    // Read initial query params
    const queryParams = this.route.snapshot.queryParams;
    if (queryParams['tab']) {
      const tabNum = Number(queryParams['tab']);
      if (!isNaN(tabNum) && tabNum >= 0 && tabNum <= 2) {
        this.activeTab.set(tabNum);
      }
    }
    if (queryParams['needsReview'] === 'true') {
      this.needsMyReview.set(true);
    }
    if (queryParams['userId']) {
      this.selectedUserId = Number(queryParams['userId']);
    }
    if (queryParams['projectId']) {
      this.selectedProjectId = Number(queryParams['projectId']);
    }

    this.refreshCurrentTab();
  }

  loadFilterOptions(): void {
    this.projectService.listActive().subscribe({
      next: (projects) => this.activeProjects.set(projects),
      error: () => {},
    });

    this.teamDashboardService.listTeamMembers().subscribe({
      next: (members) => this.teamMembers.set(members),
      error: () => {},
    });
  }

  onTabChange(index: number): void {
    this.activeTab.set(index);
    this.syncUrlParams();
    this.refreshCurrentTab();
  }

  onWeekChange(): void {
    this.syncUrlParams();
    this.refreshCurrentTab();
  }

  onRangeFilterChange(): void {
    this.rangePageIndex.set(0);
    this.loadRangeView();
  }

  onFilterChange(): void {
    this.syncUrlParams();
    this.refreshCurrentTab();
  }

  toggleNeedsMyReview(): void {
    this.needsMyReview.set(!this.needsMyReview());
    this.syncUrlParams();
    this.refreshCurrentTab();
  }

  resetFilters(): void {
    this.selectedUserId = null;
    this.selectedProjectId = null;
    this.needsMyReview.set(false);
    this.selectedWeekDate = this.getNearestMonday(new Date());
    this.rangeFrom = null;
    this.rangeTo = null;
    this.syncUrlParams();
    this.refreshCurrentTab();
  }

  refreshCurrentTab(): void {
    const tab = this.activeTab();
    if (tab === 0) {
      this.loadWeekView();
    } else if (tab === 1) {
      this.loadRangeView();
    } else if (tab === 2) {
      this.loadSectionView();
    }
  }

  loadWeekView(): void {
    this.isLoadingWeek.set(true);
    const weekStr = this.formatDate(this.selectedWeekDate);

    this.teamDashboardService
      .getTeamStatus(weekStr, {
        projectId: this.selectedProjectId,
        userId: this.selectedUserId,
      })
      .subscribe({
        next: (rows) => {
          this.weekRows.set(rows);
          this.isLoadingWeek.set(false);
        },
        error: () => {
          this.weekRows.set([]);
          this.isLoadingWeek.set(false);
        },
      });
  }

  loadRangeView(): void {
    this.isLoadingRange.set(true);

    const filters: ManagerReportFilterParams = {
      page: this.rangePageIndex(),
      size: this.rangePageSize(),
      userId: this.selectedUserId,
      projectId: this.selectedProjectId,
      status: this.needsMyReview() ? 'SUBMITTED' : undefined,
      weekStartFrom: this.rangeFrom ? this.formatDate(this.rangeFrom) : undefined,
      weekStartTo: this.rangeTo ? this.formatDate(this.rangeTo) : undefined,
    };

    this.managerReportService.list(filters).subscribe({
      next: (res) => {
        this.rangeReports.set(res.content);
        this.totalRangeElements.set(res.totalElements);
        this.isLoadingRange.set(false);
      },
      error: () => {
        this.rangeReports.set([]);
        this.totalRangeElements.set(0);
        this.isLoadingRange.set(false);
      },
    });
  }

  loadSectionView(): void {
    this.isLoadingSection.set(true);
    const weekStr = this.formatDate(this.selectedWeekDate);

    this.teamDashboardService
      .getSection(weekStr, this.selectedSection(), {
        projectId: this.selectedProjectId,
        userId: this.selectedUserId,
      })
      .subscribe({
        next: (rows) => {
          this.sectionRows.set(rows);
          this.isLoadingSection.set(false);
        },
        error: () => {
          this.sectionRows.set([]);
          this.isLoadingSection.set(false);
        },
      });
  }

  onSectionChange(sec: SectionType): void {
    this.selectedSection.set(sec);
    this.loadSectionView();
  }

  onRangePageChange(event: PageEvent): void {
    this.rangePageIndex.set(event.pageIndex);
    this.rangePageSize.set(event.pageSize);
    this.loadRangeView();
  }

  filteredWeekRows(): TeamWeekStatusRow[] {
    const rows = this.weekRows();
    if (!this.needsMyReview()) {
      return rows;
    }
    return rows.filter((r) => r.status === 'SUBMITTED');
  }

  filteredSectionRows(): SectionRow[] {
    const rows = this.sectionRows();
    if (!this.needsMyReview()) {
      return rows;
    }
    return rows.filter((r) => r.reportStatus === 'SUBMITTED');
  }

  canOpenReport(status: string): boolean {
    return status === 'SUBMITTED' || status === 'NEEDS_CORRECTION' || status === 'APPROVED';
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  private syncUrlParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        tab: this.activeTab(),
        needsReview: this.needsMyReview() ? true : null,
        userId: this.selectedUserId,
        projectId: this.selectedProjectId,
      },
      queryParamsHandling: 'merge',
    });
  }

  private getNearestMonday(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
