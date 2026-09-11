import { AccountStatus } from './user.model';

export interface TeamMemberStats {
  totalReportsSubmitted: number;
  approvedCount: number;
  needsCorrectionSentBackCount: number;
  currentWeekStatus: string;
  totalHoursLogged: number;
  totalTasksCompleted: number;
}

export interface TeamMemberProfile {
  id: number;
  fullName: string;
  email: string;
  status: AccountStatus;
  createdAt: string;
  approvedAt?: string;
  stats: TeamMemberStats;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
