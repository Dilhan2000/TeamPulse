import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { AccountStatusComponent } from './features/auth/account-status/account-status.component';
import { UnauthorizedComponent } from './shared/components/unauthorized/unauthorized.component';
import { AppShellComponent } from './layout/app-shell/app-shell.component';
import { PendingApprovalsComponent } from './features/users/pending-approvals/pending-approvals.component';
import { UserManagementComponent } from './features/users/user-management/user-management.component';
import { ReportHistoryComponent } from './features/reports/report-history/report-history.component';
import { ReportFormComponent } from './features/reports/report-form/report-form.component';
import { ReportDetailComponent } from './features/reports/report-detail/report-detail.component';
import { ManagerDashboardComponent } from './features/dashboard/manager-dashboard/manager-dashboard.component';
import { TeamReportsComponent } from './features/dashboard/team-reports/team-reports.component';
import { ProjectManagementComponent } from './features/projects/project-management/project-management.component';
import { ReportReviewComponent } from './features/review/report-review/report-review.component';
import { TeamMemberProfileComponent } from './features/users/team-member-profile/team-member-profile.component';
import { AccountSettingsComponent } from './features/account/account-settings/account-settings.component';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Public routes
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'account-status', component: AccountStatusComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },

  // Authenticated shell routes
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'manager/dashboard',
        component: ManagerDashboardComponent,
        canActivate: [roleGuard(['MANAGER'])],
      },
      {
        path: 'dashboard',
        pathMatch: 'full',
        redirectTo: 'manager/dashboard',
      },
      {
        path: 'reports',
        component: ReportHistoryComponent,
        canActivate: [roleGuard(['TEAM_MEMBER', 'MANAGER'])],
      },
      {
        path: 'reports/new',
        component: ReportFormComponent,
        canActivate: [roleGuard(['TEAM_MEMBER', 'MANAGER'])],
      },
      {
        path: 'reports/:id/edit',
        component: ReportFormComponent,
        canActivate: [roleGuard(['TEAM_MEMBER', 'MANAGER'])],
      },
      {
        path: 'reports/:id',
        component: ReportDetailComponent,
        canActivate: [roleGuard(['TEAM_MEMBER', 'MANAGER'])],
      },
      {
        path: 'manager/team-reports',
        component: TeamReportsComponent,
        canActivate: [roleGuard(['MANAGER'])],
      },
      {
        path: 'manager/projects',
        component: ProjectManagementComponent,
        canActivate: [roleGuard(['MANAGER'])],
      },
      {
        path: 'projects',
        pathMatch: 'full',
        redirectTo: 'manager/projects',
      },
      {
        path: 'manager/review-queue',
        pathMatch: 'full',
        redirectTo: 'manager/team-reports',
      },
      {
        path: 'manager/reports/:id/review',
        component: ReportReviewComponent,
        canActivate: [roleGuard(['MANAGER'])],
      },
      {
        path: 'manager/team-members/:id',
        component: TeamMemberProfileComponent,
        canActivate: [roleGuard(['MANAGER'])],
      },
      {
        path: 'account/settings',
        component: AccountSettingsComponent,
      },
      {
        path: 'admin/users/pending',
        component: PendingApprovalsComponent,
        canActivate: [roleGuard(['ADMIN'])],
      },
      {
        path: 'admin/users',
        component: UserManagementComponent,
        canActivate: [roleGuard(['ADMIN'])],
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => {
          const authService = inject(AuthService);
          const user = authService.currentUser();
          if (user?.role === 'ADMIN') {
            return 'admin/users/pending';
          } else if (user?.role === 'MANAGER') {
            return 'manager/dashboard';
          }
          return 'reports';
        },
      },
    ],
  },

  // Fallback
  { path: '**', redirectTo: 'login' },
];
