import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ManagerProjectService } from '../../../core/services/manager-project.service';
import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { Project, ProjectMember } from '../../../core/models/project.model';
import { TeamMemberOption } from '../../../core/models/report.model';

export interface ProjectTeamDialogData {
  project: Project;
}

@Component({
  selector: 'app-project-team-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './project-team-dialog.component.html',
  styleUrl: './project-team-dialog.component.scss',
})
export class ProjectTeamDialogComponent implements OnInit {
  private readonly managerProjectService = inject(ManagerProjectService);
  private readonly teamDashboardService = inject(TeamDashboardService);
  private readonly snackBar = inject(MatSnackBar);

  assignedMembers = signal<ProjectMember[]>([]);
  allActiveMembers = signal<TeamMemberOption[]>([]);
  isLoading = signal<boolean>(false);
  isAssigning = signal<boolean>(false);
  selectedUserId: number | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ProjectTeamDialogData,
    public dialogRef: MatDialogRef<ProjectTeamDialogComponent>
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    this.teamDashboardService.listTeamMembers().subscribe({
      next: (members) => {
        this.allActiveMembers.set(members);
      },
      error: () => {},
    });

    this.managerProjectService.getMembers(this.data.project.id).subscribe({
      next: (members) => {
        this.assignedMembers.set(members);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  availableMembers(): TeamMemberOption[] {
    const assignedIds = new Set(this.assignedMembers().map((m) => m.id));
    return this.allActiveMembers().filter((m) => !assignedIds.has(m.id));
  }

  onAssign(): void {
    if (!this.selectedUserId || this.isAssigning()) {
      return;
    }

    const userId = this.selectedUserId;
    this.isAssigning.set(true);

    this.managerProjectService.assignMember(this.data.project.id, userId).subscribe({
      next: () => {
        this.isAssigning.set(false);
        this.selectedUserId = null;
        this.snackBar.open('Team member assigned successfully', 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.isAssigning.set(false);
        this.snackBar.open(err.error?.message || 'Failed to assign team member', 'Close', {
          duration: 4000,
        });
      },
    });
  }

  onRemove(member: ProjectMember): void {
    this.managerProjectService.removeMember(this.data.project.id, member.id).subscribe({
      next: () => {
        this.snackBar.open(`Removed ${member.fullName}`, 'Close', { duration: 3000 });
        this.loadData();
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to remove member', 'Close', {
          duration: 4000,
        });
      },
    });
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((n) => n.charAt(0))
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }
}
