import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ActivityFeedItem,
  DashboardSummary,
  StatusByMember,
  TimeByTaskType,
  TrendPoint,
  WorkloadByProject,
} from '../models/dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardMetricsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/manager/dashboard`;

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`, { withCredentials: true });
  }

  getTasksCompletedTrend(weeks = 8, userId?: number): Observable<TrendPoint[]> {
    let params = new HttpParams().set('weeks', weeks.toString());
    if (userId != null) {
      params = params.set('userId', userId.toString());
    }
    return this.http.get<TrendPoint[]>(`${this.baseUrl}/charts/tasks-completed-trend`, {
      params,
      withCredentials: true,
    });
  }

  getStatusByMember(weekStartFrom?: string, weekStartTo?: string): Observable<StatusByMember[]> {
    let params = new HttpParams();
    if (weekStartFrom) {
      params = params.set('weekStartFrom', weekStartFrom);
    }
    if (weekStartTo) {
      params = params.set('weekStartTo', weekStartTo);
    }
    return this.http.get<StatusByMember[]>(`${this.baseUrl}/charts/status-by-member`, {
      params,
      withCredentials: true,
    });
  }

  getWorkloadByProject(weekStartFrom?: string, weekStartTo?: string): Observable<WorkloadByProject[]> {
    let params = new HttpParams();
    if (weekStartFrom) {
      params = params.set('weekStartFrom', weekStartFrom);
    }
    if (weekStartTo) {
      params = params.set('weekStartTo', weekStartTo);
    }
    return this.http.get<WorkloadByProject[]>(`${this.baseUrl}/charts/workload-by-project`, {
      params,
      withCredentials: true,
    });
  }

  getTimeByTaskType(weekStartFrom?: string, weekStartTo?: string): Observable<TimeByTaskType[]> {
    let params = new HttpParams();
    if (weekStartFrom) {
      params = params.set('weekStartFrom', weekStartFrom);
    }
    if (weekStartTo) {
      params = params.set('weekStartTo', weekStartTo);
    }
    return this.http.get<TimeByTaskType[]>(`${this.baseUrl}/charts/time-by-task-type`, {
      params,
      withCredentials: true,
    });
  }

  getActivityFeed(limit = 20): Observable<ActivityFeedItem[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ActivityFeedItem[]>(`${this.baseUrl}/activity-feed`, {
      params,
      withCredentials: true,
    });
  }
}
