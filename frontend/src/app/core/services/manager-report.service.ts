import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApproveReportRequest,
  ManagerReportFilterParams,
  Report,
  ReportReview,
  ReportSummary,
  ReportVersionSummary,
  RequestChangesRequest,
} from '../models/report.model';
import { PageResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class ManagerReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/manager/reports`;

  list(filters?: ManagerReportFilterParams): Observable<PageResponse<ReportSummary>> {
    let params = new HttpParams();

    if (filters?.page !== undefined) {
      params = params.set('page', filters.page.toString());
    }
    if (filters?.size !== undefined) {
      params = params.set('size', filters.size.toString());
    }
    if (filters?.status) {
      params = params.set('status', filters.status);
    }
    if (filters?.projectId) {
      params = params.set('projectId', filters.projectId.toString());
    }
    if (filters?.userId) {
      params = params.set('userId', filters.userId.toString());
    }
    if (filters?.weekStartDate) {
      params = params.set('weekStartDate', filters.weekStartDate);
    }

    return this.http.get<PageResponse<ReportSummary>>(this.baseUrl, {
      params,
      withCredentials: true,
    });
  }

  getById(id: number): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/${id}`, {
      withCredentials: true,
    });
  }

  approve(id: number, req?: ApproveReportRequest): Observable<Report> {
    return this.http.post<Report>(`${this.baseUrl}/${id}/approve`, req || {}, {
      withCredentials: true,
    });
  }

  requestChanges(id: number, req: RequestChangesRequest): Observable<Report> {
    return this.http.post<Report>(`${this.baseUrl}/${id}/request-changes`, req, {
      withCredentials: true,
    });
  }

  getVersions(id: number): Observable<ReportVersionSummary[]> {
    return this.http.get<ReportVersionSummary[]>(`${this.baseUrl}/${id}/versions`, {
      withCredentials: true,
    });
  }

  getVersion(id: number, versionId: number): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/${id}/versions/${versionId}`, {
      withCredentials: true,
    });
  }

  getReviews(id: number): Observable<ReportReview[]> {
    return this.http.get<ReportReview[]>(`${this.baseUrl}/${id}/reviews`, {
      withCredentials: true,
    });
  }
}
