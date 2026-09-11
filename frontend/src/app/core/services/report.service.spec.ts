import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReportService } from './report.service';
import { environment } from '../../../environments/environment';
import {
  CreateReportRequest,
  Report,
  ReportSummary,
  UpdateReportRequest,
} from '../models/report.model';
import { PageResponse } from '../models/user.model';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  const mockReport: Report = {
    id: 10,
    userId: 2,
    userName: 'Alice Member',
    projectId: 1,
    projectName: 'Client A',
    weekStartDate: '2026-09-07',
    weekEndDate: '2026-09-13',
    status: 'DRAFT',
    notes: 'Draft notes',
    submittedAt: null,
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
        ReportService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('createDraft() should send POST /api/reports and return created Report', () => {
    const reqPayload: CreateReportRequest = { projectId: 1, weekStartDate: '2026-09-07' };

    service.createDraft(reqPayload).subscribe((report) => {
      expect(report).toEqual(mockReport);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(reqPayload);
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockReport);
  });

  it('createDraft() handles 409 Conflict when report already exists for week', () => {
    const reqPayload: CreateReportRequest = { projectId: 1, weekStartDate: '2026-09-07' };

    service.createDraft(reqPayload).subscribe({
      next: () => fail('Should have failed with 409 Conflict'),
      error: (error) => {
        expect(error.status).toBe(409);
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports`);
    req.flush({ message: 'A report already exists for this week' }, { status: 409, statusText: 'Conflict' });
  });

  it('updateDraft() should send PUT /api/reports/{id}', () => {
    const updatePayload: UpdateReportRequest = {
      projectId: 1,
      notes: 'Updated notes',
      tasksCompleted: [],
      tasksPlannedNextWeek: [],
      blockers: [],
      achievements: [],
      hoursByType: [],
    };

    service.updateDraft(10, updatePayload).subscribe((report) => {
      expect(report.notes).toBe('Draft notes');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatePayload);
    req.flush(mockReport);
  });

  it('submit() should send POST /api/reports/{id}/submit', () => {
    const submittedReport: Report = { ...mockReport, status: 'SUBMITTED', submittedAt: '2026-09-09T12:00:00' };

    service.submit(10).subscribe((report) => {
      expect(report.status).toBe('SUBMITTED');
      expect(report.submittedAt).toBeDefined();
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10/submit`);
    expect(req.request.method).toBe('POST');
    req.flush(submittedReport);
  });

  it('getById() should send GET /api/reports/{id}', () => {
    service.getById(10).subscribe((report) => {
      expect(report.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10`);
    expect(req.request.method).toBe('GET');
    req.flush(mockReport);
  });

  it('getById() handles 404 Not Found for non-existent or foreign report', () => {
    service.getById(999).subscribe({
      next: () => fail('Should have returned 404'),
      error: (error) => {
        expect(error.status).toBe(404);
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/999`);
    req.flush({ message: 'Report not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('list() should send GET /api/reports with query params', () => {
    const mockPage: PageResponse<ReportSummary> = {
      content: [
        {
          id: 10,
          weekStartDate: '2026-09-07',
          weekEndDate: '2026-09-13',
          projectId: 1,
          projectName: 'Client A',
          status: 'DRAFT',
          submittedAt: null,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    };

    service.list({ page: 0, size: 10, status: 'DRAFT' }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.totalElements).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports?page=0&size=10&status=DRAFT`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('getVersions() should send GET /api/reports/:id/versions', () => {
    service.getVersions(10).subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10/versions`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([{ id: 1, versionNumber: 1, submittedAt: '2026-09-08T10:00:00' }]);
  });

  it('getVersion() should send GET /api/reports/:id/versions/:versionId', () => {
    service.getVersion(10, 1).subscribe((res) => {
      expect(res.id).toBe(10);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10/versions/1`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockReport);
  });

  it('getReviews() should send GET /api/reports/:id/reviews', () => {
    service.getReviews(10).subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/reports/10/reviews`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush([
      {
        id: 1,
        reportId: 10,
        reviewerId: 1,
        reviewerName: 'Bob Manager',
        action: 'APPROVED',
        comment: 'LGTM',
        reviewedAt: '2026-09-08T11:00:00',
        versionId: 1,
        versionNumber: 1,
      },
    ]);
  });
});
