import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { AuthService } from '../../../core/services/auth.service';
import { ChangePasswordRequest } from '../../../core/models/user-profile.model';

export function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const newPass = group.get('newPassword')?.value;
  const confirmPass = group.get('confirmPassword')?.value;
  if (newPass && confirmPass && newPass !== confirmPass) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressBarModule,
  ],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss'],
})
export class AccountSettingsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly currentUser = this.authService.currentUser;
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly hideCurrent = signal<boolean>(true);
  readonly hideNew = signal<boolean>(true);
  readonly hideConfirm = signal<boolean>(true);

  passwordForm: FormGroup = this.fb.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,}$/),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator }
  );

  ngOnInit(): void {
    if (!this.currentUser()) {
      this.authService.fetchCurrentUser().subscribe();
    }
  }

  toggleHideCurrent(): void {
    this.hideCurrent.update((v) => !v);
  }

  toggleHideNew(): void {
    this.hideNew.update((v) => !v);
  }

  toggleHideConfirm(): void {
    this.hideConfirm.update((v) => !v);
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValues = this.passwordForm.value;
    const req: ChangePasswordRequest = {
      currentPassword: formValues.currentPassword,
      newPassword: formValues.newPassword,
    };

    this.authService.changePassword(req).subscribe({
      next: (res) => {
        this.submitting.set(false);
        const msg = res?.message || 'Password updated successfully. Other active sessions have been signed out.';
        this.successMessage.set(msg);
        this.snackBar.open('Password changed successfully!', 'Close', {
          duration: 4000,
          horizontalPosition: 'end',
          verticalPosition: 'top',
        });
        this.passwordForm.reset();
        Object.keys(this.passwordForm.controls).forEach((key) => {
          this.passwordForm.get(key)?.setErrors(null);
        });
      },
      error: (err) => {
        this.submitting.set(false);
        const msg =
          err.error?.message ||
          (err.status === 400 ? 'Incorrect current password or invalid inputs' : 'Failed to update password. Please try again.');
        this.errorMessage.set(msg);
      },
    });
  }

  getInitials(name?: string): string {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }

  formatRole(role?: string): string {
    if (!role) return '';
    switch (role) {
      case 'TEAM_MEMBER':
        return 'Team Member';
      case 'MANAGER':
        return 'Manager';
      case 'ADMIN':
        return 'Administrator';
      default:
        return role;
    }
  }
}
