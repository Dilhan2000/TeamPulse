import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountStatus,
  ApproveUserRequest,
  PageResponse,
  Role,
  UpdateUserRoleRequest,
  UpdateUserStatusRequest,
  User,
} from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/users`;

  getPendingUsers(page = 0, size = 10): Observable<PageResponse<User>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<User>>(`${this.baseUrl}/pending`, {
      params,
      withCredentials: true,
    });
  }

  approveUser(id: number, role?: Role): Observable<User> {
    const payload: ApproveUserRequest = role ? { role } : {};
    return this.http.post<User>(`${this.baseUrl}/${id}/approve`, payload, {
      withCredentials: true,
    });
  }

  rejectUser(id: number, reason?: string): Observable<User> {
    return this.http.post<User>(
      `${this.baseUrl}/${id}/reject`,
      { reason },
      { withCredentials: true },
    );
  }

  getUsers(
    status?: AccountStatus | null,
    role?: Role | null,
    search?: string | null,
    page = 0,
    size = 10,
  ): Observable<PageResponse<User>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }
    if (role) {
      params = params.set('role', role);
    }
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PageResponse<User>>(this.baseUrl, { params, withCredentials: true });
  }

  updateUserStatus(id: number, status: 'ACTIVE' | 'DISABLED'): Observable<User> {
    const payload: UpdateUserStatusRequest = { status };
    return this.http.patch<User>(`${this.baseUrl}/${id}/status`, payload, {
      withCredentials: true,
    });
  }

  updateUserRole(id: number, role: 'TEAM_MEMBER' | 'MANAGER'): Observable<User> {
    const payload: UpdateUserRoleRequest = { role };
    return this.http.patch<User>(`${this.baseUrl}/${id}/role`, payload, { withCredentials: true });
  }
}
