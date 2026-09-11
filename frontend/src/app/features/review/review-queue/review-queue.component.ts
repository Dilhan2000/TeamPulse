import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ManagerReportService } from '../../../core/services/manager-report.service';
import { ProjectService } from '../../../core/services/project.service';
import {
  ManagerReportFilterParams,
  Project,
  ReportStatus,
  ReportSummary,
} from '../../../core/models/report.model';

@Component({
  selector: 'app-review-queue',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './review-queue.component.html',
  styleUrl: './review-queue.component.scss',
})
export class ReviewQueueComponent implements OnInit {
  private readonly managerReportService = inject(ManagerReportService);
  private readonly projectService = inject(ProjectService);

  reports = signal<ReportSummary[]>([]);
  projects = signal<Project[]>([]);
  totalElements = signal<number>(0);
  isLoading = signal<boolean>(true);

  pageIndex = signal<number>(0);
  pageSize = signal<number>(10);

  selectedStatus: ReportStatus | null = 'SUBMITTED';
  selectedProjectId: number | null = null;

  displayedColumns: string[] = [
    'member',
    'project',
    'week',
    'submittedAt',
    'status',
    'actions',
  ];

  ngOnInit(): void {
    this.loadProjects();
    this.fetchReports();
  }

  loadProjects(): void {
    this.projectService.listAll().subscribe({
      next: (data) => this.projects.set(data),
      error: () => {},
    });
  }

  fetchReports(): void {
    this.isLoading.set(true);

    const filters: ManagerReportFilterParams = {
      page: this.pageIndex(),
      size: this.pageSize(),
      status: this.selectedStatus || undefined,
      projectId: this.selectedProjectId || undefined,
    };

    this.managerReportService.list(filters).subscribe({
      next: (res) => {
        this.reports.set(res.content);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.reports.set([]);
        this.totalElements.set(0);
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.pageIndex.set(0);
    this.fetchReports();
  }

  resetFilters(): void {
    this.selectedStatus = 'SUBMITTED';
    this.selectedProjectId = null;
    this.pageIndex.set(0);
    this.fetchReports();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.fetchReports();
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
}
