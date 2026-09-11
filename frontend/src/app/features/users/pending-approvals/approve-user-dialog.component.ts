import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { Role, User } from '../../../core/models/user.model';

@Component({
  selector: 'app-approve-user-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
  ],
  templateUrl: './approve-user-dialog.component.html',
  styleUrl: './approve-user-dialog.component.scss',
})
export class ApproveUserDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ApproveUserDialogComponent>);
  readonly data = inject<{ user: User }>(MAT_DIALOG_DATA);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    role: [this.data.user.role],
  });

  onConfirm(): void {
    const selectedRole: Role = this.form.value.role;
    this.dialogRef.close({ confirmed: true, role: selectedRole });
  }
}
