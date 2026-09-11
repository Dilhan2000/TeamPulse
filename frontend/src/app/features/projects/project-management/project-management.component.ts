import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ManagerProjectService } from '../../../core/services/manager-project.service';
import { Project, ProjectFilterParams } from '../../../core/models/project.model';
import { ProjectDialogComponent } from '../project-dialog/project-dialog.component';
import { ProjectTeamDialogComponent } from '../project-team-dialog/project-team-dialog.component';

@Component({
  selector: 'app-project-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './project-management.component.html',
  styleUrl: './project-management.component.scss',
})
export class ProjectManagementComponent implements OnInit {
  private readonly managerProjectService = inject(ManagerProjectService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  projects = signal<Project[]>([]);
  totalElements = signal<number>(0);
  pageIndex = signal<number>(0);
  pageSize = signal<number>(10);
  isLoading = signal<boolean>(false);

  searchQuery = '';
  statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';

  displayedColumns: string[] = ['name', 'description', 'status', 'actions'];

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParams;
    if (qp['search']) {
      this.searchQuery = qp['search'];
    }
    if (qp['status']) {
      this.statusFilter = qp['status'];
    }
    if (qp['page']) {
      this.pageIndex.set(Number(qp['page']) || 0);
    }

    this.loadProjects();
  }

  loadProjects(): void {
    this.isLoading.set(true);

    const activeParam =
      this.statusFilter === 'ACTIVE'
        ? true
        : this.statusFilter === 'INACTIVE'
        ? false
        : null;

    const params: ProjectFilterParams = {
      search: this.searchQuery || null,
      active: activeParam,
      page: this.pageIndex(),
      size: this.pageSize(),
    };

    this.managerProjectService.list(params).subscribe({
      next: (res) => {
        this.projects.set(res.content);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.projects.set([]);
        this.totalElements.set(0);
        this.isLoading.set(false);
      },
    });
  }

  onSearchChange(): void {
    this.pageIndex.set(0);
    this.syncUrlParams();
    this.loadProjects();
  }

  onStatusFilterChange(): void {
    this.pageIndex.set(0);
    this.syncUrlParams();
    this.loadProjects();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.syncUrlParams();
    this.loadProjects();
  }

  onToggleStatus(project: Project, newStatus: boolean): void {
    // Immediate UI feedback
    this.managerProjectService.setStatus(project.id, newStatus).subscribe({
      next: (updated) => {
        project.active = updated.active;
        this.snackBar.open(
          `Project "${project.name}" is now ${project.active ? 'Active' : 'Inactive'}`,
          'Close',
          { duration: 3000 }
        );
        // If filtering by active/inactive, remove from current view
        if (
          (this.statusFilter === 'ACTIVE' && !project.active) ||
          (this.statusFilter === 'INACTIVE' && project.active)
        ) {
          this.loadProjects();
        }
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Failed to update project status', 'Close', {
          duration: 4000,
        });
        // Revert toggle
        project.active = !newStatus;
      },
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '480px',
      data: {},
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadProjects();
      }
    });
  }

  openEditDialog(project: Project): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '480px',
      data: { project },
    });

    dialogRef.afterClosed().subscribe((result: Project | undefined) => {
      if (result) {
        // Update row immediately
        const list = [...this.projects()];
        const idx = list.findIndex((p) => p.id === result.id);
        if (idx !== -1) {
          list[idx] = result;
          this.projects.set(list);
        } else {
          this.loadProjects();
        }
      }
    });
  }

  openTeamDialog(project: Project): void {
    this.dialog.open(ProjectTeamDialogComponent, {
      width: '520px',
      data: { project },
    });
  }

  private syncUrlParams(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        search: this.searchQuery || null,
        status: this.statusFilter !== 'ALL' ? this.statusFilter : null,
        page: this.pageIndex() > 0 ? this.pageIndex() : null,
      },
      queryParamsHandling: 'merge',
    });
  }
}
