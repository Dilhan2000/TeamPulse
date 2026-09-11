export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskProgressStatus =
  | 'NOT_STARTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'BLOCKED'
  | 'DEFERRED';

export type TaskType =
  | 'DEVELOPMENT'
  | 'TESTING'
  | 'MEETINGS'
  | 'DOCUMENTATION'
  | 'OTHER';

export type ReportStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'NEEDS_CORRECTION'
  | 'APPROVED';

export interface Project {
  id: number;
  name: string;
  description?: string | null;
  active?: boolean;
}

export interface TaskItem {
  id?: number;
  taskName: string;
  priority: TaskPriority;
  plannedPercent: number;
  actualPercent: number;
  status: TaskProgressStatus;
  timePlannedHours: number;
  timeSpentHours: number;
  deliverable?: string | null;
  sortOrder?: number;
}

export interface NextWeekTask {
  id?: number;
  description: string;
  sortOrder?: number;
}

export interface Blocker {
  id?: number;
  description: string;
  isKeyIssue: boolean;
  resolved: boolean;
  sortOrder?: number;
}

export interface Achievement {
  id?: number;
  description: string;
  isKeyAchievement: boolean;
  sortOrder?: number;
}

export interface HoursByType {
  taskType: TaskType;
  hours: number;
}

export type ReviewAction = 'APPROVED' | 'CHANGES_REQUESTED';

export interface ReportReview {
  id: number;
  reportId: number;
  reviewerId: number;
  reviewerName: string;
  action: ReviewAction;
  comment?: string | null;
  reviewedAt: string;
  versionId?: number | null;
  versionNumber?: number | null;
}

export interface ReportVersionSummary {
  id: number;
  versionNumber: number;
  submittedAt: string;
}

export interface ApproveReportRequest {
  comment?: string;
}

export interface RequestChangesRequest {
  comment: string;
}

export interface Report {
  id: number;
  userId: number;
  userName: string;
  projectId: number;
  projectName: string;
  weekStartDate: string;
  weekEndDate: string;
  status: ReportStatus;
  notes?: string | null;
  submittedAt?: string | null;
  createdAt?: string;
  tasksCompleted: TaskItem[];
  tasksPlannedNextWeek: NextWeekTask[];
  blockers: Blocker[];
  achievements: Achievement[];
  hoursByType: HoursByType[];
  latestReview?: ReportReview | null;
}

export interface ReportSummary {
  id: number;
  weekStartDate: string;
  weekEndDate: string;
  projectId: number;
  projectName: string;
  userId?: number;
  userName?: string;
  status: ReportStatus;
  submittedAt?: string | null;
}

export interface CreateReportRequest {
  projectId: number;
  weekStartDate: string;
}

export interface UpdateReportRequest {
  projectId: number;
  notes?: string | null;
  tasksCompleted: TaskItem[];
  tasksPlannedNextWeek: NextWeekTask[];
  blockers: Blocker[];
  achievements: Achievement[];
  hoursByType: HoursByType[];
}

export interface ReportFilterParams {
  status?: ReportStatus | null;
  weekStartFrom?: string | null;
  weekStartTo?: string | null;
  page?: number;
  size?: number;
}

export interface ManagerReportFilterParams {
  status?: ReportStatus | null;
  projectId?: number | null;
  userId?: number | null;
  weekStartDate?: string | null;
  weekStartFrom?: string | null;
  weekStartTo?: string | null;
  page?: number;
  size?: number;
}

export type TeamWeekStatus = ReportStatus | 'NOT_STARTED';

export interface TeamWeekStatusRow {
  userId: number;
  fullName: string;
  reportId?: number | null;
  projectId?: number | null;
  projectName?: string | null;
  status: TeamWeekStatus;
  submittedAt?: string | null;
}

export type SectionType = 'BLOCKERS' | 'ACHIEVEMENTS';

export interface SectionItem {
  description: string;
  flagged: boolean;
  resolved?: boolean | null;
}

export interface SectionRow {
  userId: number;
  fullName: string;
  projectName: string;
  reportId: number;
  reportStatus: ReportStatus;
  items: SectionItem[];
}

export interface TeamMemberOption {
  id: number;
  fullName: string;
}

export interface TeamStatusFilterParams {
  projectId?: number | null;
  userId?: number | null;
}


