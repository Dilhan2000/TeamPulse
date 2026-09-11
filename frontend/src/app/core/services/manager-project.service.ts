import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateProjectRequest,
  Project,
  ProjectFilterParams,
  ProjectMember,
  UpdateProjectRequest,
  UpdateProjectStatusRequest,
} from '../models/project.model';
import { PageResponse } from '../models/user.model';

/**
 * Service for manager project CRUD and team assignment (C5-T10, C5-B04).
 */
@Injectable({
  providedIn: 'root',
})
export class ManagerProjectService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/manager/projects`;

  list(params?: ProjectFilterParams): Observable<PageResponse<Project>> {
    let httpParams = new HttpParams();
    if (params?.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params?.active !== undefined && params?.active !== null) {
      httpParams = httpParams.set('active', params.active.toString());
    }
    if (params?.page !== undefined && params?.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params?.size !== undefined && params?.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<PageResponse<Project>>(this.baseUrl, {
      params: httpParams,
      withCredentials: true,
    });
  }

  create(req: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, req, {
      withCredentials: true,
    });
  }

  update(id: number, req: UpdateProjectRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, req, {
      withCredentials: true,
    });
  }

  setStatus(id: number, active: boolean): Observable<Project> {
    const req: UpdateProjectStatusRequest = { active };
    return this.http.patch<Project>(`${this.baseUrl}/${id}/status`, req, {
      withCredentials: true,
    });
  }

  getMembers(projectId: number): Observable<ProjectMember[]> {
    return this.http.get<ProjectMember[]>(`${this.baseUrl}/${projectId}/members`, {
      withCredentials: true,
    });
  }

  assignMember(projectId: number, userId: number): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${projectId}/members`,
      { userId },
      { withCredentials: true }
    );
  }

  removeMember(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/members/${userId}`, {
      withCredentials: true,
    });
  }
}
