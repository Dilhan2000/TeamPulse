import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { ReportService } from '../../../core/services/report.service';
import { ManagerReportService } from '../../../core/services/manager-report.service';
import { Report, ReportVersionSummary } from '../../../core/models/report.model';

export interface VersionDialogData {
  reportId: number;
  isManager?: boolean;
}

@Component({
  selector: 'app-version-history-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDividerModule,
    DatePipe,
  ],
  templateUrl: './version-history-dialog.component.html',
  styleUrl: './version-history-dialog.component.scss',
})
export class VersionHistoryDialogComponent implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<VersionHistoryDialogComponent>);
  readonly data: VersionDialogData = inject(MAT_DIALOG_DATA);
  private readonly reportService = inject(ReportService);
  private readonly managerReportService = inject(ManagerReportService);

  versions = signal<ReportVersionSummary[]>([]);
  selectedVersionId = signal<number | null>(null);
  snapshotData = signal<Report | null>(null);
  isLoadingList = signal<boolean>(true);
  isLoadingSnapshot = signal<boolean>(false);

  taskColumns: string[] = ['taskName', 'priority', 'actual', 'status', 'hours'];

  ngOnInit(): void {
    this.loadVersions();
  }

  loadVersions(): void {
    this.isLoadingList.set(true);
    const versions$ = this.data.isManager
      ? this.managerReportService.getVersions(this.data.reportId)
      : this.reportService.getVersions(this.data.reportId);

    versions$.subscribe({
      next: (list) => {
        this.versions.set(list);
        this.isLoadingList.set(false);
        if (list.length > 0) {
          this.selectVersion(list[0].id);
        }
      },
      error: () => {
        this.isLoadingList.set(false);
      },
    });
  }

  selectVersion(versionId: number): void {
    this.selectedVersionId.set(versionId);
    this.isLoadingSnapshot.set(true);

    const version$ = this.data.isManager
      ? this.managerReportService.getVersion(this.data.reportId, versionId)
      : this.reportService.getVersion(this.data.reportId, versionId);

    version$.subscribe({
      next: (report) => {
        this.snapshotData.set(report);
        this.isLoadingSnapshot.set(false);
      },
      error: () => {
        this.isLoadingSnapshot.set(false);
      },
    });
  }

  getSelectedVersionNumber(): number {
    const found = this.versions().find((v) => v.id === this.selectedVersionId());
    return found ? found.versionNumber : 1;
  }

  computeHours(report: Report): number {
    if (!report.tasksCompleted || report.tasksCompleted.length === 0) {
      if (report.hoursByType) {
        return report.hoursByType.reduce((acc, curr) => acc + Number(curr.hours), 0);
      }
      return 0;
    }
    return report.tasksCompleted.reduce((acc, curr) => acc + Number(curr.timeSpentHours || 0), 0);
  }
}
