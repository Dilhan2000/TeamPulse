import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ReportService } from '../../../core/services/report.service';
import { ReportFilterParams, ReportStatus, ReportSummary } from '../../../core/models/report.model';

@Component({
  selector: 'app-report-history',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatInputModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './report-history.component.html',
  styleUrl: './report-history.component.scss',
})
export class ReportHistoryComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly router = inject(Router);

  displayedColumns: string[] = ['week', 'project', 'status', 'submittedAt', 'actions'];

  reports = signal<ReportSummary[]>([]);
  isLoading = signal<boolean>(false);
  totalElements = signal<number>(0);

  selectedStatus: ReportStatus | null = null;
  dateFrom: Date | null = null;
  dateTo: Date | null = null;

  pageIndex = 0;
  pageSize = 10;

  ngOnInit(): void {
    this.fetchReports();
  }

  fetchReports(): void {
    this.isLoading.set(true);

    const filters: ReportFilterParams = {
      page: this.pageIndex,
      size: this.pageSize,
      status: this.selectedStatus,
      weekStartFrom: this.dateFrom ? this.formatDate(this.dateFrom) : null,
      weekStartTo: this.dateTo ? this.formatDate(this.dateTo) : null,
    };

    this.reportService.list(filters).subscribe({
      next: (response) => {
        this.reports.set(response.content || []);
        const total = response.page?.totalElements ?? response.totalElements ?? (response.content ? response.content.length : 0);
        this.totalElements.set(total);
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
    this.pageIndex = 0;
    this.fetchReports();
  }

  resetFilters(): void {
    this.selectedStatus = null;
    this.dateFrom = null;
    this.dateTo = null;
    this.pageIndex = 0;
    this.fetchReports();
  }

  hasActiveFilters(): boolean {
    return this.selectedStatus !== null || this.dateFrom !== null || this.dateTo !== null;
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchReports();
  }

  formatStatus(status: ReportStatus): string {
    return status.replace('_', ' ');
  }

  private formatDate(date: Date): string {
    const d = new Date(date);
    const month = '' + (d.getMonth() + 1);
    const day = '' + d.getDate();
    const year = d.getFullYear();

    return [year, month.padStart(2, '0'), day.padStart(2, '0')].join('-');
  }
}
