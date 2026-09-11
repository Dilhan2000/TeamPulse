import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuthService } from '../../../core/services/auth.service';
import { AccountStatus, Role, User } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressBarModule,
    MatSnackBarModule,
  ],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss',
})
export class UserManagementComponent implements OnInit {
  private readonly adminService = inject(AdminUserService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly displayedColumns: string[] = ['fullName', 'email', 'role', 'status', 'createdAt'];

  readonly users = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly totalElements = signal(0);
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly currentAdminId = signal<number | null>(null);

  readonly filterForm: FormGroup = this.fb.group({
    search: [''],
    role: [null],
    status: [null],
  });

  ngOnInit(): void {
    const currentUser = this.authService.currentUser();
    if (currentUser) {
      this.currentAdminId.set(currentUser.id);
    }

    // Read initial query params
    const qp = this.route.snapshot.queryParams;
    if (qp['search']) this.filterForm.patchValue({ search: qp['search'] });
    if (qp['role']) this.filterForm.patchValue({ role: qp['role'] });
    if (qp['status']) this.filterForm.patchValue({ status: qp['status'] });

    this.loadUsers();

    // Listen to filter changes with debounce
    this.filterForm.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.pageIndex.set(0);
      this.syncQueryParams();
      this.loadUsers();
    });
  }

  loadUsers(): void {
    this.isLoading.set(true);
    const { search, role, status } = this.filterForm.value;

    this.adminService.getUsers(status, role, search, this.pageIndex(), this.pageSize()).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.users.set(res.content);
        this.totalElements.set(res.totalElements);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Failed to load users list.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadUsers();
  }

  resetFilters(): void {
    this.filterForm.reset({ search: '', role: null, status: null });
    this.syncQueryParams();
    this.loadUsers();
  }

  onRoleChange(user: User, newRole: 'TEAM_MEMBER' | 'MANAGER'): void {
    this.adminService.updateUserRole(user.id, newRole).subscribe({
      next: (updated) => {
        this.snackBar.open(
          `Updated role for ${user.fullName} to ${newRole.replace('_', ' ')}.`,
          'Close',
          {
            duration: 3000,
          },
        );
        user.role = updated.role;
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update role.', 'Dismiss', {
          duration: 4000,
        });
        this.loadUsers();
      },
    });
  }

  onStatusToggle(user: User, isChecked: boolean): void {
    const newStatus: AccountStatus = isChecked ? 'ACTIVE' : 'DISABLED';
    this.adminService.updateUserStatus(user.id, newStatus).subscribe({
      next: (updated) => {
        const label = updated.status === 'ACTIVE' ? 'enabled' : 'disabled';
        this.snackBar.open(`Account for ${user.fullName} has been ${label}.`, 'Close', {
          duration: 3000,
        });
        user.status = updated.status;
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update status.', 'Dismiss', {
          duration: 4000,
        });
        this.loadUsers();
      },
    });
  }

  formatStatus(status: string): string {
    return status.replace('_', ' ');
  }

  private syncQueryParams(): void {
    const { search, role, status } = this.filterForm.value;
    const queryParams: Record<string, string | null> = {};
    if (search) queryParams['search'] = search;
    if (role) queryParams['role'] = role;
    if (status) queryParams['status'] = status;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }
}
