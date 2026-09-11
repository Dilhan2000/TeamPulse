import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ManagerProjectService } from '../../../core/services/manager-project.service';
import { Project } from '../../../core/models/project.model';

export interface ProjectDialogData {
  project?: Project;
}

@Component({
  selector: 'app-project-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './project-dialog.component.html',
  styleUrl: './project-dialog.component.scss',
})
export class ProjectDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly managerProjectService = inject(ManagerProjectService);
  private readonly dialogRef = inject(MatDialogRef<ProjectDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);

  isEdit = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  form!: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: ProjectDialogData) {}

  ngOnInit(): void {
    const existing = this.data?.project;
    this.isEdit.set(!!existing);

    this.form = this.fb.group({
      name: [
        existing?.name || '',
        [Validators.required, Validators.maxLength(150)],
      ],
      description: [existing?.description || '', [Validators.maxLength(500)]],
    });

    this.form.get('name')?.valueChanges.subscribe(() => {
      if (this.form.get('name')?.hasError('duplicate')) {
        const errors = { ...this.form.get('name')?.errors };
        delete errors['duplicate'];
        this.form.get('name')?.setErrors(Object.keys(errors).length ? errors : null);
      }
      this.errorMessage.set(null);
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const formValue = this.form.value;
    const name = formValue.name.trim();
    const description = formValue.description?.trim() || null;

    if (this.isEdit()) {
      const id = this.data.project!.id;
      this.managerProjectService.update(id, { name, description }).subscribe({
        next: (updated) => {
          this.isSubmitting.set(false);
          this.snackBar.open('Project updated successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(updated);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          if (err.status === 409) {
            this.form.get('name')?.setErrors({ duplicate: true });
          } else {
            this.errorMessage.set(err.error?.message || 'Failed to update project. Please try again.');
          }
        },
      });
    } else {
      this.managerProjectService.create({ name, description }).subscribe({
        next: (created) => {
          this.isSubmitting.set(false);
          this.snackBar.open('Project created successfully', 'Close', { duration: 3000 });
          this.dialogRef.close(created);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          if (err.status === 409) {
            this.form.get('name')?.setErrors({ duplicate: true });
          } else {
            this.errorMessage.set(err.error?.message || 'Failed to create project. Please try again.');
          }
        },
      });
    }
  }
}
