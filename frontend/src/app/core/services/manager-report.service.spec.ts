import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ManagerReportService } from './manager-report.service';
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

describe('ManagerReportService', () => {
  let service: ManagerReportService;
  let httpMock: HttpTestingController;

  const mockReport: Report = {
    id: 10,
    userId: 2,
    userName: 'Alice Member',
    projectId: 1,
    projectName: 'Client A',
    weekStartDate: '2026-09-07',
    weekEndDate: '2026-09-13',
    status: 'SUBMITTED',
    notes: 'Submitted report',
    submittedAt: '2026-09-08T10:00:00',
    createdAt: '2026-09-07T10:00:00',
    tasksCompleted: [],
    tasksPlannedNextWeek: [],
    blockers: [],
    achievements: [],
    hoursByType: [],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ManagerReportService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ManagerReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('list() should send GET /api/manager/reports with filters', () => {
    const filters: ManagerReportFilterParams = {
      status: 'SUBMITTED',
      projectId: 1,
      userId: 2,
      page: 0,
      size: 10,
    };

    const mockResponse: PageResponse<ReportSummary> = {
      content: [
        {
          id: 10,
          weekStartDate: '2026-09-07',
          weekEndDate: '2026-09-13',
          projectId: 1,
          projectName: 'Client A',
          status: 'SUBMITTED',
          submittedAt: '2026-09-08T10:00:00',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false,
    };

    service.list(filters).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].id).toBe(10);
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/manager/reports` &&
        r.params.get('status') === 'SUBMITTED' &&
        r.params.get('projectId') === '1' &&
        r.params.get('userId') === '2' &&
        r.params.get('page') === '0' &&
        r.params.get('size') === '10'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockResponse);
  });

  it('getById() should send GET /api/manager/reports/:id', () => {
    service.getById(10).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockReport);
  });

  it('approve() should send POST /api/manager/reports/:id/approve', () => {
    const approveReq: ApproveReportRequest = { comment: 'Looks great!' };
    service.approve(10, approveReq).subscribe((res) => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(approveReq);
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ ...mockReport, status: 'APPROVED' });
  });

  it('requestChanges() should send POST /api/manager/reports/:id/request-changes', () => {
    const changesReq: RequestChangesRequest = { comment: 'Please update deliverable' };
    service.requestChanges(10, changesReq).subscribe((res) => {
      expect(res.status).toBe('NEEDS_CORRECTION');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10/request-changes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(changesReq);
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ ...mockReport, status: 'NEEDS_CORRECTION' });
  });

  it('getVersions() should send GET /api/manager/reports/:id/versions', () => {
    const mockVersions: ReportVersionSummary[] = [
      { id: 1, versionNumber: 1, submittedAt: '2026-09-08T10:00:00' },
    ];

    service.getVersions(10).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].versionNumber).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10/versions`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockVersions);
  });

  it('getVersion() should send GET /api/manager/reports/:id/versions/:versionId', () => {
    service.getVersion(10, 1).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10/versions/1`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockReport);
  });

  it('getReviews() should send GET /api/manager/reports/:id/reviews', () => {
    const mockReviews: ReportReview[] = [
      {
        id: 1,
        reportId: 10,
        reviewerId: 1,
        reviewerName: 'Bob Manager',
        action: 'CHANGES_REQUESTED',
        comment: 'Fix notes',
        reviewedAt: '2026-09-08T11:00:00',
        versionId: 1,
        versionNumber: 1,
      },
    ];

    service.getReviews(10).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].action).toBe('CHANGES_REQUESTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/reports/10/reviews`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockReviews);
  });
});
