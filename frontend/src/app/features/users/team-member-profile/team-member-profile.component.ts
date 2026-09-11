import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { ManagerReportService } from '../../../core/services/manager-report.service';
import { TeamMemberProfile } from '../../../core/models/user-profile.model';
import { ReportSummary } from '../../../core/models/report.model';

@Component({
  selector: 'app-team-member-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './team-member-profile.component.html',
  styleUrls: ['./team-member-profile.component.scss'],
})
export class TeamMemberProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly teamDashboardService = inject(TeamDashboardService);
  private readonly managerReportService = inject(ManagerReportService);

  userId = signal<number | null>(null);
  loadingProfile = signal<boolean>(true);
  loadingReports = signal<boolean>(true);

  profile = signal<TeamMemberProfile | null>(null);
  reports = signal<ReportSummary[]>([]);
  totalReports = signal<number>(0);
  pageIndex = signal<number>(0);
  pageSize = signal<number>(10);

  displayedColumns: string[] = ['week', 'project', 'submittedAt', 'status', 'actions'];

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const idParam = params.get('id');
      if (idParam) {
        const id = parseInt(idParam, 10);
        this.userId.set(id);
        this.loadProfile(id);
        this.loadReports(id);
      }
    });
  }

  loadProfile(userId: number): void {
    this.loadingProfile.set(true);
    this.teamDashboardService.getProfile(userId).subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.loadingProfile.set(false);
      },
      error: (err) => {
        console.error('Failed to load team member profile', err);
        this.loadingProfile.set(false);
      },
    });
  }

  loadReports(userId: number): void {
    this.loadingReports.set(true);
    this.managerReportService
      .list({
        userId,
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .subscribe({
        next: (page) => {
          this.reports.set(page.content);
          this.totalReports.set(page.totalElements);
          this.loadingReports.set(false);
        },
        error: (err) => {
          console.error('Failed to load user reports', err);
          this.loadingReports.set(false);
        },
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    const id = this.userId();
    if (id) {
      this.loadReports(id);
    }
  }

  openReport(report: ReportSummary): void {
    if (report.status === 'DRAFT') {
      return; // Draft content is private
    }
    this.router.navigate(['/manager/reports', report.id, 'review']);
  }

  goBack(): void {
    this.router.navigate(['/manager/team-reports']);
  }

  getInitials(name: string): string {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'badge-approved';
      case 'SUBMITTED':
        return 'badge-submitted';
      case 'NEEDS_CORRECTION':
        return 'badge-correction';
      case 'DRAFT':
        return 'badge-draft';
      case 'NOT_STARTED':
      default:
        return 'badge-not-started';
    }
  }

  formatStatus(status: string): string {
    switch (status) {
      case 'NEEDS_CORRECTION':
        return 'Needs Correction';
      case 'NOT_STARTED':
        return 'Not Started';
      default:
        return status.charAt(0) + status.slice(1).toLowerCase();
    }
  }
}
