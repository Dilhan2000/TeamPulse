/**
 * Project domain models and DTOs (C5-T09).
 */

export interface Project {
  id: number;
  name: string;
  description?: string | null;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string | null;
}

export interface UpdateProjectRequest {
  name: string;
  description?: string | null;
}

export interface UpdateProjectStatusRequest {
  active: boolean;
}

export interface ProjectFilterParams {
  search?: string | null;
  active?: boolean | null;
  page?: number;
  size?: number;
}

export interface ProjectMember {
  id: number;
  fullName: string;
}
