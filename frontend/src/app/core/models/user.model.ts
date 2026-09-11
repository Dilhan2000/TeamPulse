export type Role = 'TEAM_MEMBER' | 'MANAGER' | 'ADMIN';

export type AccountStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'REJECTED' | 'DISABLED';

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  status: AccountStatus;
  createdAt?: string;
  approvedAt?: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  requestedRole: Role;
}

export interface MessageResponse {
  message: string;
}

export interface ApproveUserRequest {
  role?: Role;
}

export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'DISABLED';
}

export interface UpdateUserRoleRequest {
  role: 'TEAM_MEMBER' | 'MANAGER';
}

export interface PageResponse<T> {
  content: T[];
  pageable?: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
