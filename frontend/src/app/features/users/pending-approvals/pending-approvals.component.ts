import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { User } from '../../../core/models/user.model';
import { ApproveUserDialogComponent } from './approve-user-dialog.component';

@Component({
  selector: 'app-pending-approvals',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './pending-approvals.component.html',
  styleUrl: './pending-approvals.component.scss',
})
export class PendingApprovalsComponent implements OnInit {
  private readonly adminService = inject(AdminUserService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns: string[] = [
    'fullName',
    'email',
    'requestedRole',
    'createdAt',
    'actions',
  ];

  readonly users = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly totalElements = signal(0);
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);

  ngOnInit(): void {
    this.loadPendingUsers();
  }

  loadPendingUsers(): void {
    this.isLoading.set(true);
    this.adminService.getPendingUsers(this.pageIndex(), this.pageSize()).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.users.set(res.content);
        this.totalElements.set(res.totalElements);
      },
      error: () => {
        this.isLoading.set(false);
        this.snackBar.open('Failed to load pending users.', 'Dismiss', { duration: 4000 });
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadPendingUsers();
  }

  formatRole(role: string): string {
    return role.replace('_', ' ');
  }

  openApproveDialog(user: User): void {
    const dialogRef = this.dialog.open(ApproveUserDialogComponent, {
      width: '420px',
      data: { user },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.confirmed) {
        this.adminService.approveUser(user.id, result.role).subscribe({
          next: () => {
            this.snackBar.open(`User ${user.fullName} approved successfully!`, 'Close', {
              duration: 3500,
            });
            this.loadPendingUsers();
          },
          error: (err) => {
            this.snackBar.open(err.error?.message || 'Approval failed.', 'Dismiss', {
              duration: 4000,
            });
          },
        });
      }
    });
  }

  confirmReject(user: User): void {
    if (confirm(`Are you sure you want to reject registration for ${user.fullName}?`)) {
      this.adminService.rejectUser(user.id).subscribe({
        next: () => {
          this.snackBar.open(`User ${user.fullName} registration rejected.`, 'Close', {
            duration: 3500,
          });
          this.loadPendingUsers();
        },
        error: (err) => {
          this.snackBar.open(err.error?.message || 'Rejection failed.', 'Dismiss', {
            duration: 4000,
          });
        },
      });
    }
  }
}
