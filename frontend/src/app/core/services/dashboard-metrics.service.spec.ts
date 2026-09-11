import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardMetricsService } from './dashboard-metrics.service';
import { environment } from '../../../environments/environment';
import {
  DashboardSummary,
  TrendPoint,
  StatusByMember,
  WorkloadByProject,
  TimeByTaskType,
  ActivityFeedItem,
} from '../models/dashboard.model';

describe('DashboardMetricsService', () => {
  let service: DashboardMetricsService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/manager/dashboard`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardMetricsService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(DashboardMetricsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch summary metrics', () => {
    const mockSummary: DashboardSummary = {
      submittedThisWeek: 4,
      compliance: { submitted: 4, pending: 1, late: 0, totalActiveMembers: 5 },
      needsCorrectionCount: 2,
      openBlockersCount: 1,
      currentWeekStartDate: '2026-09-07',
    };

    service.getSummary().subscribe((data) => {
      expect(data).toEqual(mockSummary);
      expect(data.submittedThisWeek).toBe(4);
    });

    const req = httpMock.expectOne(`${baseUrl}/summary`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockSummary);
  });

  it('should fetch tasks completed trend with default weeks and optional userId', () => {
    const mockTrend: TrendPoint[] = [
      { weekStartDate: '2026-08-31', completedCount: 5 },
      { weekStartDate: '2026-09-07', completedCount: 8 },
    ];

    service.getTasksCompletedTrend(4, 12).subscribe((data) => {
      expect(data).toEqual(mockTrend);
    });

    const req = httpMock.expectOne(`${baseUrl}/charts/tasks-completed-trend?weeks=4&userId=12`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockTrend);
  });

  it('should fetch status by member', () => {
    const mockStatus: StatusByMember[] = [
      { userId: 1, fullName: 'Alice Member', draft: 1, submitted: 0, needsCorrection: 0, approved: 0 },
    ];

    service.getStatusByMember('2026-08-31', '2026-09-07').subscribe((data) => {
      expect(data).toEqual(mockStatus);
    });

    const req = httpMock.expectOne(
      `${baseUrl}/charts/status-by-member?weekStartFrom=2026-08-31&weekStartTo=2026-09-07`
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockStatus);
  });

  it('should fetch workload by project', () => {
    const mockWorkload: WorkloadByProject[] = [
      { projectId: 1, projectName: 'Project Apollo', totalHours: 40 },
    ];

    service.getWorkloadByProject('2026-09-01', '2026-09-07').subscribe((data) => {
      expect(data).toEqual(mockWorkload);
    });

    const req = httpMock.expectOne(
      `${baseUrl}/charts/workload-by-project?weekStartFrom=2026-09-01&weekStartTo=2026-09-07`
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockWorkload);
  });

  it('should fetch time by task type', () => {
    const mockTime: TimeByTaskType[] = [
      { taskType: 'DEVELOPMENT', totalHours: 25 },
      { taskType: 'TESTING', totalHours: 10 },
      { taskType: 'MEETINGS', totalHours: 5 },
      { taskType: 'DOCUMENTATION', totalHours: 0 },
      { taskType: 'OTHER', totalHours: 0 },
    ];

    service.getTimeByTaskType().subscribe((data) => {
      expect(data).toEqual(mockTime);
      expect(data.length).toBe(5);
    });

    const req = httpMock.expectOne(`${baseUrl}/charts/time-by-task-type`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockTime);
  });

  it('should fetch activity feed', () => {
    const mockFeed: ActivityFeedItem[] = [
      {
        id: 'SUBMISSION-1',
        type: 'SUBMISSION',
        actorName: 'Bob',
        reportId: 10,
        projectName: 'Alpha',
        weekStartDate: '2026-09-07',
        action: 'SUBMITTED',
        timestamp: '2026-09-08T10:00:00Z',
      },
    ];

    service.getActivityFeed(10).subscribe((data) => {
      expect(data).toEqual(mockFeed);
    });

    const req = httpMock.expectOne(`${baseUrl}/activity-feed?limit=10`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockFeed);
  });
});

