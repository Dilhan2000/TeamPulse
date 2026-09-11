import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ManagerReportService } from '../../../core/services/manager-report.service';
import { Report, ReportReview } from '../../../core/models/report.model';
import { CorrectionBannerComponent } from '../../../shared/components/correction-banner/correction-banner.component';
import { ReviewHistoryComponent } from '../../../shared/components/review-history/review-history.component';
import { VersionHistoryDialogComponent } from '../../../shared/components/version-history/version-history-dialog.component';
import {
  ReviewActionDialogComponent,
  ReviewActionResult,
} from '../review-action-dialog/review-action-dialog.component';

@Component({
  selector: 'app-report-review',
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
    MatSnackBarModule,
    MatDialogModule,
    DatePipe,
    CorrectionBannerComponent,
    ReviewHistoryComponent,
  ],
  templateUrl: './report-review.component.html',
  styleUrl: './report-review.component.scss',
})
export class ReportReviewComponent implements OnInit {
  private readonly managerReportService = inject(ManagerReportService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  report = signal<Report | null>(null);
  reviews = signal<ReportReview[]>([]);
  isLoading = signal<boolean>(true);
  isSubmittingAction = signal<boolean>(false);

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
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const reportId = Number(idParam);
      if (!isNaN(reportId)) {
        this.loadReport(reportId);
        this.loadReviews(reportId);
      }
    }
  }

  loadReport(id: number): void {
    this.isLoading.set(true);
    this.managerReportService.getById(id).subscribe({
      next: (data) => {
        this.report.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Report not found or access denied', 'Close', { duration: 4000 });
        this.router.navigate(['/manager/team-reports']);
      },
    });
  }

  loadReviews(id: number): void {
    this.managerReportService.getReviews(id).subscribe({
      next: (data) => this.reviews.set(data),
      error: () => {},
    });
  }

  openVersionHistory(reportId: number): void {
    this.dialog.open(VersionHistoryDialogComponent, {
      data: { reportId, isManager: true },
      width: '880px',
      maxWidth: '95vw',
    });
  }

  onApprove(rep: Report): void {
    const dialogRef = this.dialog.open<ReviewActionDialogComponent, any, ReviewActionResult>(
      ReviewActionDialogComponent,
      {
        data: {
          action: 'APPROVE',
          reportWeek: rep.weekStartDate,
          memberName: rep.userName,
        },
      }
    );

    dialogRef.afterClosed().subscribe((res) => {
      if (res?.confirmed) {
        this.isSubmittingAction.set(true);
        this.managerReportService.approve(rep.id, { comment: res.comment }).subscribe({
          next: (updated) => {
            this.report.set(updated);
            this.loadReviews(rep.id);
            this.isSubmittingAction.set(false);
            this.snackBar.open('Report successfully approved', 'Close', { duration: 4000 });
          },
          error: (err) => {
            this.isSubmittingAction.set(false);
            const msg = err.error?.message || 'Failed to approve report';
            this.snackBar.open(msg, 'Close', { duration: 4000 });
          },
        });
      }
    });
  }

  onRequestChanges(rep: Report): void {
    const dialogRef = this.dialog.open<ReviewActionDialogComponent, any, ReviewActionResult>(
      ReviewActionDialogComponent,
      {
        data: {
          action: 'REQUEST_CHANGES',
          reportWeek: rep.weekStartDate,
          memberName: rep.userName,
        },
      }
    );

    dialogRef.afterClosed().subscribe((res) => {
      if (res?.confirmed && res.comment) {
        this.isSubmittingAction.set(true);
        this.managerReportService.requestChanges(rep.id, { comment: res.comment }).subscribe({
          next: (updated) => {
            this.report.set(updated);
            this.loadReviews(rep.id);
            this.isSubmittingAction.set(false);
            this.snackBar.open('Correction request sent to team member', 'Close', { duration: 4000 });
          },
          error: (err) => {
            this.isSubmittingAction.set(false);
            const msg = err.error?.message || 'Failed to request changes';
            this.snackBar.open(msg, 'Close', { duration: 4000 });
          },
        });
      }
    });
  }

  formatStatus(status: string): string {
    return status.replace(/_/g, ' ');
  }

  formatTaskType(type: string): string {
    return type.charAt(0) + type.slice(1).toLowerCase();
  }

  computeTotalHours(rep: Report): number {
    if (!rep.tasksCompleted || rep.tasksCompleted.length === 0) {
      if (rep.hoursByType) {
        return rep.hoursByType.reduce((acc, curr) => acc + Number(curr.hours), 0);
      }
      return 0;
    }
    return rep.tasksCompleted.reduce((acc, curr) => acc + Number(curr.timeSpentHours || 0), 0);
  }
}
