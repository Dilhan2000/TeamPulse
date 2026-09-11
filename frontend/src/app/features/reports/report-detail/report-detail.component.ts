import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ReportService } from '../../../core/services/report.service';
import { Report, ReportReview, ReportStatus, TaskPriority, TaskProgressStatus } from '../../../core/models/report.model';
import { CorrectionBannerComponent } from '../../../shared/components/correction-banner/correction-banner.component';
import { ReviewHistoryComponent } from '../../../shared/components/review-history/review-history.component';
import { VersionHistoryDialogComponent } from '../../../shared/components/version-history/version-history-dialog.component';

@Component({
  selector: 'app-report-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDialogModule,
    CorrectionBannerComponent,
    ReviewHistoryComponent,
  ],
  templateUrl: './report-detail.component.html',
  styleUrl: './report-detail.component.scss',
})
export class ReportDetailComponent implements OnInit {
  private readonly reportService = inject(ReportService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  @Input() report: Report | null = null;

  reportData = signal<Report | null>(null);
  reviews = signal<ReportReview[]>([]);
  isLoading = signal<boolean>(false);

  taskColumns: string[] = [
    'taskName',
    'priority',
    'planned',
    'actual',
    'status',
    'hoursPlanned',
    'hoursSpent',
  ];

  ngOnInit(): void {
    if (this.report) {
      this.reportData.set(this.report);
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const reportId = Number(idParam);
      if (!isNaN(reportId)) {
        this.fetchReport(reportId);
        this.fetchReviews(reportId);
      }
    }
  }

  fetchReport(id: number): void {
    this.isLoading.set(true);
    this.reportService.getById(id).subscribe({
      next: (data) => {
        this.reportData.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.router.navigate(['/reports']);
      },
    });
  }

  fetchReviews(id: number): void {
    this.reportService.getReviews(id).subscribe({
      next: (revs) => this.reviews.set(revs),
      error: () => {},
    });
  }

  openVersionHistory(reportId: number): void {
    this.dialog.open(VersionHistoryDialogComponent, {
      data: { reportId, isManager: false },
      width: '880px',
      maxWidth: '95vw',
    });
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  formatTaskType(type: string): string {
    return type.charAt(0) + type.slice(1).toLowerCase();
  }

  computeTotalHours(report: Report): number {
    if (!report.tasksCompleted || report.tasksCompleted.length === 0) {
      // Check hoursByType
      if (report.hoursByType && report.hoursByType.length > 0) {
        return report.hoursByType.reduce((acc: number, curr) => acc + Number(curr.hours), 0);
      }
      return 0;
    }
    return report.tasksCompleted.reduce((acc: number, curr) => acc + Number(curr.timeSpentHours || 0), 0);
  }
}
