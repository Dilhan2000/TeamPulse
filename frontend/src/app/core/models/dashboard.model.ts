export interface ComplianceMetrics {
  submitted: number;
  pending: number;
  late: number;
  totalActiveTeamMembers?: number;
  totalActiveMembers?: number;
}

export interface DashboardSummary {
  totalSubmittedThisWeek?: number;
  submittedThisWeek?: number;
  compliance: ComplianceMetrics;
  needsCorrectionCount: number;
  openBlockersCount: number;
  currentWeekStartDate?: string;
}

export interface TrendPoint {
  weekStartDate: string;
  completedCount: number;
}

export interface StatusByMember {
  userId: number;
  fullName: string;
  draft: number;
  submitted: number;
  needsCorrection: number;
  approved: number;
}

export interface WorkloadByProject {
  projectId: number;
  projectName: string;
  totalHours: number;
}

export type TaskType = 'DEVELOPMENT' | 'TESTING' | 'MEETINGS' | 'DOCUMENTATION' | 'OTHER';

export interface TimeByTaskType {
  taskType: TaskType;
  totalHours: number;
}

export interface ActivityFeedItem {
  id?: string;
  type: 'SUBMISSION' | 'REVIEW';
  actorName: string;
  targetUserName?: string;
  reportId?: number;
  projectName?: string;
  weekStartDate: string;
  action?: string;
  timestamp: string;
}
