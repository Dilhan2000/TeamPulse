import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly hidePassword = signal(true);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.loginForm.value;

    this.authService.login(email, password).subscribe({
      next: (user) => {
        this.isLoading.set(false);
        const returnUrl: string | undefined = this.route.snapshot.queryParams['returnUrl'];
        const isForbiddenForAdmin =
          user.role === 'ADMIN' &&
          Boolean(
            returnUrl &&
              (returnUrl === '/' ||
                returnUrl === '/reports' ||
                returnUrl.startsWith('/reports') ||
                returnUrl.startsWith('/manager')),
          );

        if (returnUrl && !isForbiddenForAdmin) {
          this.router.navigateByUrl(returnUrl);
        } else if (user.role === 'ADMIN') {
          this.router.navigate(['/admin/users/pending']);
        } else if (user.role === 'MANAGER') {
          this.router.navigate(['/manager/dashboard']);
        } else {
          this.router.navigate(['/reports']);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Invalid email or password.');
        } else if (err.status === 403) {
          this.errorMessage.set(err.error?.message || 'Access is not permitted for your account.');
        } else {
          this.errorMessage.set(
            err.error?.message || 'A network error occurred. Please try again.',
          );
        }
      },
    });
  }
}
