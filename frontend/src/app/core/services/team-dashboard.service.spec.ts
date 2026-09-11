import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TeamDashboardService } from './team-dashboard.service';
import { environment } from '../../../environments/environment';
import {
  SectionRow,
  TeamMemberOption,
  TeamWeekStatusRow,
} from '../models/report.model';

describe('TeamDashboardService', () => {
  let service: TeamDashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TeamDashboardService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(TeamDashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getTeamStatus() should send GET /api/manager/dashboard/team-status with params', () => {
    const mockRows: TeamWeekStatusRow[] = [
      {
        userId: 1,
        fullName: 'Alice Member',
        reportId: 10,
        projectId: 2,
        projectName: 'Client A',
        status: 'SUBMITTED',
        submittedAt: '2026-09-08T10:00:00',
      },
      {
        userId: 2,
        fullName: 'Bob Member',
        reportId: null,
        projectId: null,
        projectName: null,
        status: 'NOT_STARTED',
        submittedAt: null,
      },
    ];

    service.getTeamStatus('2026-09-07', { projectId: 2, userId: 1 }).subscribe((rows) => {
      expect(rows.length).toBe(2);
      expect(rows[0].status).toBe('SUBMITTED');
      expect(rows[1].status).toBe('NOT_STARTED');
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/manager/dashboard/team-status` &&
        r.params.get('weekStart') === '2026-09-07' &&
        r.params.get('projectId') === '2' &&
        r.params.get('userId') === '1'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockRows);
  });

  it('getSection() should send GET /api/manager/dashboard/section with params', () => {
    const mockSections: SectionRow[] = [
      {
        userId: 1,
        fullName: 'Alice Member',
        projectName: 'Client A',
        reportId: 10,
        reportStatus: 'SUBMITTED',
        items: [
          { description: 'Blocker A', flagged: true, resolved: false },
        ],
      },
    ];

    service.getSection('2026-09-07', 'BLOCKERS', { projectId: 2 }).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].items[0].flagged).toBeTrue();
    });

    const req = httpMock.expectOne(
      (r) =>
        r.url === `${environment.apiBaseUrl}/manager/dashboard/section` &&
        r.params.get('weekStart') === '2026-09-07' &&
        r.params.get('section') === 'BLOCKERS' &&
        r.params.get('projectId') === '2'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockSections);
  });

  it('listTeamMembers() should send GET /api/manager/team-members', () => {
    const mockMembers: TeamMemberOption[] = [
      { id: 1, fullName: 'Alice Member' },
      { id: 2, fullName: 'Bob Member' },
    ];

    service.listTeamMembers().subscribe((members) => {
      expect(members.length).toBe(2);
      expect(members[0].fullName).toBe('Alice Member');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/team-members`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockMembers);
  });

  it('getProfile() should send GET /api/manager/team-members/:id', () => {
    const mockProfile = {
      id: 5,
      fullName: 'Alice Member',
      email: 'alice@test.com',
      status: 'ACTIVE',
      createdAt: '2026-08-01T10:00:00',
      stats: {
        totalReportsSubmitted: 4,
        approvedCount: 3,
        needsCorrectionSentBackCount: 1,
        currentWeekStatus: 'APPROVED',
        totalHoursLogged: 45.0,
        totalTasksCompleted: 12,
      },
    };

    service.getProfile(5).subscribe((profile) => {
      expect(profile.id).toBe(5);
      expect(profile.stats.totalReportsSubmitted).toBe(4);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/manager/team-members/5`);
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBeTrue();
    req.flush(mockProfile);
  });
});
