import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ProjectService } from '../../../core/services/project.service';
import { ReportService } from '../../../core/services/report.service';
import {
  HoursByType,
  Project,
  Report,
  TaskItem,
  TaskPriority,
  TaskProgressStatus,
  TaskType,
  UpdateReportRequest,
} from '../../../core/models/report.model';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { CorrectionBannerComponent } from '../../../shared/components/correction-banner/correction-banner.component';

@Component({
  selector: 'app-report-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatTooltipModule,
    CorrectionBannerComponent,
  ],
  templateUrl: './report-form.component.html',
  styleUrl: './report-form.component.scss',
})
export class ReportFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly projectService = inject(ProjectService);
  private readonly reportService = inject(ReportService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  taskTypes: TaskType[] = [
    'DEVELOPMENT',
    'TESTING',
    'MEETINGS',
    'DOCUMENTATION',
    'OTHER',
  ];

  projects = signal<Project[]>([]);
  isLoading = signal<boolean>(false);
  isSaving = signal<boolean>(false);
  isEditMode = signal<boolean>(false);
  existingReport = signal<Report | null>(null);
  serverError = signal<string | null>(null);

  reportId: number | null = null;

  reportForm: FormGroup = this.fb.group({
    projectId: [null, [Validators.required]],
    weekStartDate: [null, [Validators.required]],
    notes: ['', [Validators.maxLength(2000)]],
    tasksCompleted: this.fb.array([]),
    tasksPlannedNextWeek: this.fb.array([]),
    blockers: this.fb.array([]),
    achievements: this.fb.array([]),
    hoursByType: this.fb.group({
      DEVELOPMENT: [0, [Validators.min(0), Validators.max(168)]],
      TESTING: [0, [Validators.min(0), Validators.max(168)]],
      MEETINGS: [0, [Validators.min(0), Validators.max(168)]],
      DOCUMENTATION: [0, [Validators.min(0), Validators.max(168)]],
      OTHER: [0, [Validators.min(0), Validators.max(168)]],
    }),
  });

  get tasksCompletedArray(): FormArray {
    return this.reportForm.get('tasksCompleted') as FormArray;
  }

  get tasksPlannedNextWeekArray(): FormArray {
    return this.reportForm.get('tasksPlannedNextWeek') as FormArray;
  }

  get blockersArray(): FormArray {
    return this.reportForm.get('blockers') as FormArray;
  }

  get achievementsArray(): FormArray {
    return this.reportForm.get('achievements') as FormArray;
  }

  get hoursGroup(): FormGroup {
    return this.reportForm.get('hoursByType') as FormGroup;
  }

  mondayDateFilter = (d: Date | null): boolean => {
    const day = (d || new Date()).getDay();
    return day === 1; // 1 = Monday in JS Date
  };

  ngOnInit(): void {
    this.loadProjects();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.reportId = Number(idParam);
      this.isEditMode.set(true);
      this.reportForm.get('weekStartDate')?.clearValidators();
      this.reportForm.get('weekStartDate')?.updateValueAndValidity();
      this.loadReportForEdit(this.reportId);
    }
  }

  loadProjects(): void {
    this.projectService.listActive().subscribe({
      next: (data) => this.projects.set(data),
      error: () => this.serverError.set('Failed to load projects list.'),
    });
  }

  loadReportForEdit(id: number): void {
    this.isLoading.set(true);
    this.reportService.getById(id).subscribe({
      next: (report) => {
        // C2-T18 & C3-T07: Only DRAFT and NEEDS_CORRECTION can be edited
        if (report.status !== 'DRAFT' && report.status !== 'NEEDS_CORRECTION') {
          this.router.navigate(['/reports', id]);
          return;
        }

        this.existingReport.set(report);
        this.populateForm(report);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.serverError.set(err.error?.message || 'Failed to load report.');
        this.router.navigate(['/reports']);
      },
    });
  }

  populateForm(report: Report): void {
    this.reportForm.patchValue({
      projectId: report.projectId,
      weekStartDate: report.weekStartDate,
      notes: report.notes || '',
    });

    // Populate Tasks Completed
    this.tasksCompletedArray.clear();
    if (report.tasksCompleted) {
      report.tasksCompleted.forEach((t) => this.addTaskItem(t));
    }

    // Populate Planned Next Week
    this.tasksPlannedNextWeekArray.clear();
    if (report.tasksPlannedNextWeek) {
      report.tasksPlannedNextWeek.forEach((p) => this.addNextWeekTask(p.description));
    }

    // Populate Blockers
    this.blockersArray.clear();
    if (report.blockers) {
      report.blockers.forEach((b) => this.addBlocker(b.description, b.isKeyIssue, b.resolved));
    }

    // Populate Achievements
    this.achievementsArray.clear();
    if (report.achievements) {
      report.achievements.forEach((a) => this.addAchievement(a.description, a.isKeyAchievement));
    }

    // Populate Hours by Type
    if (report.hoursByType) {
      const hoursMap: Record<string, number> = {};
      report.hoursByType.forEach((h) => {
        hoursMap[h.taskType] = h.hours;
      });
      this.hoursGroup.patchValue(hoursMap);
    }
  }

  // --- Task Items ---
  addTaskItem(initial?: Partial<TaskItem>): void {
    const group = this.fb.group({
      taskName: [initial?.taskName || '', [Validators.required, Validators.maxLength(255)]],
      priority: [initial?.priority || 'MEDIUM', [Validators.required]],
      plannedPercent: [initial?.plannedPercent ?? 0, [Validators.required, Validators.min(0), Validators.max(100)]],
      actualPercent: [initial?.actualPercent ?? 0, [Validators.required, Validators.min(0), Validators.max(100)]],
      status: [initial?.status || 'IN_PROGRESS', [Validators.required]],
      timePlannedHours: [initial?.timePlannedHours ?? 0, [Validators.required, Validators.min(0), Validators.max(168)]],
      timeSpentHours: [initial?.timeSpentHours ?? 0, [Validators.required, Validators.min(0), Validators.max(168)]],
      deliverable: [initial?.deliverable || '', [Validators.maxLength(500)]],
    });
    this.tasksCompletedArray.push(group);
  }

  removeTaskItem(index: number): void {
    this.tasksCompletedArray.removeAt(index);
  }

  // --- Next Week Tasks ---
  addNextWeekTask(description = ''): void {
    const group = this.fb.group({
      description: [description, [Validators.required, Validators.maxLength(500)]],
    });
    this.tasksPlannedNextWeekArray.push(group);
  }

  removeNextWeekTask(index: number): void {
    this.tasksPlannedNextWeekArray.removeAt(index);
  }

  // --- Blockers ---
  addBlocker(description = '', isKeyIssue = false, resolved = false): void {
    const group = this.fb.group({
      description: [description, [Validators.required, Validators.maxLength(500)]],
      isKeyIssue: [isKeyIssue],
      resolved: [resolved],
    });
    this.blockersArray.push(group);
  }

  removeBlocker(index: number): void {
    this.blockersArray.removeAt(index);
  }

  onKeyIssueToggle(index: number, checked: boolean): void {
    if (checked) {
      // Mutual exclusion: set isKeyIssue to false for all other rows
      this.blockersArray.controls.forEach((control, i) => {
        if (i !== index) {
          control.get('isKeyIssue')?.setValue(false, { emitEvent: false });
        }
      });
    }
  }

  // --- Achievements ---
  addAchievement(description = '', isKeyAchievement = false): void {
    const group = this.fb.group({
      description: [description, [Validators.required, Validators.maxLength(500)]],
      isKeyAchievement: [isKeyAchievement],
    });
    this.achievementsArray.push(group);
  }

  removeAchievement(index: number): void {
    this.achievementsArray.removeAt(index);
  }

  onKeyAchievementToggle(index: number, checked: boolean): void {
    if (checked) {
      // Mutual exclusion: set isKeyAchievement to false for all other rows
      this.achievementsArray.controls.forEach((control, i) => {
        if (i !== index) {
          control.get('isKeyAchievement')?.setValue(false, { emitEvent: false });
        }
      });
    }
  }

  totalHoursByType(): number {
    const values = this.hoursGroup.value;
    return Object.values(values).reduce((sum: number, curr: any) => sum + (Number(curr) || 0), 0);
  }

  formatTaskType(type: string): string {
    return type.charAt(0) + type.slice(1).toLowerCase();
  }

  private buildUpdatePayload(): UpdateReportRequest {
    const formValue = this.reportForm.value;

    const hoursByType: HoursByType[] = [];
    if (formValue.hoursByType) {
      for (const [key, val] of Object.entries(formValue.hoursByType)) {
        const hrs = Number(val);
        if (hrs > 0) {
          hoursByType.push({ taskType: key as TaskType, hours: hrs });
        }
      }
    }

    return {
      projectId: formValue.projectId,
      notes: formValue.notes || null,
      tasksCompleted: (formValue.tasksCompleted || []).map((t: any, idx: number) => ({
        ...t,
        sortOrder: idx,
      })),
      tasksPlannedNextWeek: (formValue.tasksPlannedNextWeek || []).map((n: any, idx: number) => ({
        ...n,
        sortOrder: idx,
      })),
      blockers: (formValue.blockers || []).map((b: any, idx: number) => ({
        ...b,
        sortOrder: idx,
      })),
      achievements: (formValue.achievements || []).map((a: any, idx: number) => ({
        ...a,
        sortOrder: idx,
      })),
      hoursByType,
    };
  }

  saveDraft(): void {
    this.serverError.set(null);

    // If new report, weekStartDate is required
    if (!this.isEditMode()) {
      const weekControl = this.reportForm.get('weekStartDate');
      const projectControl = this.reportForm.get('projectId');
      if (!weekControl?.value || !projectControl?.value) {
        weekControl?.markAsTouched();
        projectControl?.markAsTouched();
        this.snackBar.open('Please select a Project and Week Start Date', 'Close', { duration: 4000 });
        return;
      }
    } else {
      const projectControl = this.reportForm.get('projectId');
      if (!projectControl?.value) {
        projectControl?.markAsTouched();
        this.snackBar.open('Please select a Project', 'Close', { duration: 4000 });
        return;
      }
    }

    this.isSaving.set(true);

    if (this.isEditMode() && this.reportId) {
      const payload = this.buildUpdatePayload();
      this.reportService.updateDraft(this.reportId, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.snackBar.open('Draft report saved successfully', 'OK', { duration: 3000 });
          this.router.navigate(['/reports']);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.serverError.set(err.error?.message || 'Failed to update draft report.');
        },
      });
    } else {
      // Create draft first, then update with contents
      const weekDate: Date = this.reportForm.get('weekStartDate')?.value;
      const formattedDate = this.formatDate(weekDate);

      this.reportService.createDraft({
        projectId: this.reportForm.get('projectId')?.value,
        weekStartDate: formattedDate,
      }).subscribe({
        next: (created) => {
          const payload = this.buildUpdatePayload();
          this.reportService.updateDraft(created.id, payload).subscribe({
            next: () => {
              this.isSaving.set(false);
              this.snackBar.open('Draft report saved successfully', 'OK', { duration: 3000 });
              this.router.navigate(['/reports']);
            },
            error: (err) => {
              this.isSaving.set(false);
              this.serverError.set(err.error?.message || 'Report created, but failed to save task details.');
            },
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          this.serverError.set(err.error?.message || 'Failed to create report draft.');
        },
      });
    }
  }

  confirmSubmit(): void {
    if (this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      this.snackBar.open('Please fix the validation errors before submitting.', 'Close', { duration: 4000 });
      return;
    }

    const isCorrection = this.existingReport()?.status === 'NEEDS_CORRECTION';
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: isCorrection ? 'Resubmit Weekly Report' : 'Submit Weekly Report',
        message: isCorrection
          ? 'Are you ready to resubmit this report for manager review? A new immutable version snapshot will be created.'
          : "Once submitted, you won't be able to edit this report unless your manager requests corrections. Are you sure you want to proceed?",
        confirmText: isCorrection ? 'Resubmit Now' : 'Submit Now',
        cancelText: 'Cancel',
        confirmColor: 'primary',
        icon: 'send',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.executeSubmit();
      }
    });
  }

  private executeSubmit(): void {
    this.isSaving.set(true);

    if (this.isEditMode() && this.reportId) {
      const payload = this.buildUpdatePayload();
      this.reportService.updateDraft(this.reportId, payload).subscribe({
        next: () => {
          this.reportService.submit(this.reportId!).subscribe({
            next: (submitted) => {
              this.isSaving.set(false);
              this.snackBar.open('Report submitted successfully for review!', 'OK', { duration: 4000 });
              this.router.navigate(['/reports', submitted.id]);
            },
            error: (err) => {
              this.isSaving.set(false);
              this.serverError.set(err.error?.message || 'Failed to submit report.');
            },
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          this.serverError.set(err.error?.message || 'Failed to save changes before submit.');
        },
      });
    } else {
      const weekDate: Date = this.reportForm.get('weekStartDate')?.value;
      const formattedDate = this.formatDate(weekDate);

      this.reportService.createDraft({
        projectId: this.reportForm.get('projectId')?.value,
        weekStartDate: formattedDate,
      }).subscribe({
        next: (created) => {
          const payload = this.buildUpdatePayload();
          this.reportService.updateDraft(created.id, payload).subscribe({
            next: () => {
              this.reportService.submit(created.id).subscribe({
                next: (submitted) => {
                  this.isSaving.set(false);
                  this.snackBar.open('Report submitted successfully for review!', 'OK', { duration: 4000 });
                  this.router.navigate(['/reports', submitted.id]);
                },
                error: (err) => {
                  this.isSaving.set(false);
                  this.serverError.set(err.error?.message || 'Failed to submit report.');
                },
              });
            },
            error: (err) => {
              this.isSaving.set(false);
              this.serverError.set(err.error?.message || 'Failed to save report rows.');
            },
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          this.serverError.set(err.error?.message || 'Failed to create report.');
        },
      });
    }
  }

  private formatDate(date: Date): string {
    const d = new Date(date);
    const month = '' + (d.getMonth() + 1);
    const day = '' + d.getDate();
    const year = d.getFullYear();

    return [year, month.padStart(2, '0'), day.padStart(2, '0')].join('-');
  }
}
