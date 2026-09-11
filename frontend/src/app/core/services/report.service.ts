import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateReportRequest,
  Report,
  ReportFilterParams,
  ReportReview,
  ReportSummary,
  ReportVersionSummary,
  UpdateReportRequest,
} from '../models/report.model';
import { PageResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/reports`;

  createDraft(req: CreateReportRequest): Observable<Report> {
    return this.http.post<Report>(this.baseUrl, req, {
      withCredentials: true,
    });
  }

  updateDraft(id: number, req: UpdateReportRequest): Observable<Report> {
    return this.http.put<Report>(`${this.baseUrl}/${id}`, req, {
      withCredentials: true,
    });
  }

  submit(id: number): Observable<Report> {
    return this.http.post<Report>(`${this.baseUrl}/${id}/submit`, {}, {
      withCredentials: true,
    });
  }

  getById(id: number): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/${id}`, {
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

  list(filters?: ReportFilterParams): Observable<PageResponse<ReportSummary>> {
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
    if (filters?.weekStartFrom) {
      params = params.set('weekStartFrom', filters.weekStartFrom);
    }
    if (filters?.weekStartTo) {
      params = params.set('weekStartTo', filters.weekStartTo);
    }

    return this.http.get<PageResponse<ReportSummary>>(this.baseUrl, {
      params,
      withCredentials: true,
    });
  }
}
