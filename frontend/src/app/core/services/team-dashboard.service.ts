import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SectionRow,
  SectionType,
  TeamMemberOption,
  TeamStatusFilterParams,
  TeamWeekStatusRow,
} from '../models/report.model';
import { TeamMemberProfile } from '../models/user-profile.model';

@Injectable({
  providedIn: 'root',
})
export class TeamDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/manager`;

  getTeamStatus(
    weekStart: string,
    filters?: TeamStatusFilterParams
  ): Observable<TeamWeekStatusRow[]> {
    let params = new HttpParams().set('weekStart', weekStart);

    if (filters?.projectId) {
      params = params.set('projectId', filters.projectId.toString());
    }
    if (filters?.userId) {
      params = params.set('userId', filters.userId.toString());
    }

    return this.http.get<TeamWeekStatusRow[]>(`${this.baseUrl}/dashboard/team-status`, {
      params,
      withCredentials: true,
    });
  }

  getSection(
    weekStart: string,
    section: SectionType,
    filters?: TeamStatusFilterParams
  ): Observable<SectionRow[]> {
    let params = new HttpParams()
      .set('weekStart', weekStart)
      .set('section', section);

    if (filters?.projectId) {
      params = params.set('projectId', filters.projectId.toString());
    }
    if (filters?.userId) {
      params = params.set('userId', filters.userId.toString());
    }

    return this.http.get<SectionRow[]>(`${this.baseUrl}/dashboard/section`, {
      params,
      withCredentials: true,
    });
  }

  listTeamMembers(): Observable<TeamMemberOption[]> {
    return this.http.get<TeamMemberOption[]>(`${this.baseUrl}/team-members`, {
      withCredentials: true,
    });
  }

  getProfile(userId: number): Observable<TeamMemberProfile> {
    return this.http.get<TeamMemberProfile>(`${this.baseUrl}/team-members/${userId}`, {
      withCredentials: true,
    });
  }
}
