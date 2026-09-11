import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { TeamMemberProfileComponent } from './team-member-profile.component';
import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { ManagerReportService } from '../../../core/services/manager-report.service';
import { TeamMemberProfile } from '../../../core/models/user-profile.model';
import { ReportSummary } from '../../../core/models/report.model';
import { PageResponse } from '../../../core/models/user.model';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('TeamMemberProfileComponent', () => {
  let component: TeamMemberProfileComponent;
  let fixture: ComponentFixture<TeamMemberProfileComponent>;
  let teamDashboardServiceSpy: jasmine.SpyObj<TeamDashboardService>;
  let managerReportServiceSpy: jasmine.SpyObj<ManagerReportService>;
  let router: Router;

  const mockProfile: TeamMemberProfile = {
    id: 42,
    fullName: 'Alice Bob',
    email: 'alice@example.com',
    status: 'ACTIVE',
    createdAt: '2025-01-15T08:00:00Z',
    stats: {
      totalReportsSubmitted: 12,
      approvedCount: 10,
      needsCorrectionSentBackCount: 2,
      currentWeekStatus: 'SUBMITTED',
      totalHoursLogged: 480.5,
      totalTasksCompleted: 35,
    },
  };

  const mockReports: ReportSummary[] = [
    {
      id: 101,
      userId: 42,
      userName: 'Alice Bob',
      projectId: 1,
      projectName: 'Project Alpha',
      weekStartDate: '2026-03-02',
      weekEndDate: '2026-03-08',
      submittedAt: '2026-03-06T17:00:00Z',
      status: 'APPROVED',
    },
    {
      id: 102,
      userId: 42,
      userName: 'Alice Bob',
      projectId: 1,
      projectName: 'Project Alpha',
      weekStartDate: '2026-03-09',
      weekEndDate: '2026-03-15',
      submittedAt: null,
      status: 'DRAFT',
    },
  ];

  const mockPage: PageResponse<ReportSummary> = {
    content: mockReports,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
  };

  beforeEach(async () => {
    teamDashboardServiceSpy = jasmine.createSpyObj('TeamDashboardService', ['getProfile']);
    managerReportServiceSpy = jasmine.createSpyObj('ManagerReportService', ['list']);

    teamDashboardServiceSpy.getProfile.and.returnValue(of(mockProfile));
    managerReportServiceSpy.list.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [TeamMemberProfileComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: TeamDashboardService, useValue: teamDashboardServiceSpy },
        { provide: ManagerReportService, useValue: managerReportServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ id: '42' })),
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(TeamMemberProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load profile and reports on init', () => {
    expect(component).toBeTruthy();
    expect(teamDashboardServiceSpy.getProfile).toHaveBeenCalledWith(42);
    expect(managerReportServiceSpy.list).toHaveBeenCalledWith({
      userId: 42,
      page: 0,
      size: 10,
    });
    expect(component.profile()).toEqual(mockProfile);
    expect(component.reports().length).toBe(2);
  });

  it('should display member details in header', () => {
    const el = fixture.nativeElement as HTMLElement;
    const nameEl = el.querySelector('.member-name');
    expect(nameEl?.textContent).toContain('Alice Bob');

    const avatarEl = el.querySelector('.member-avatar');
    expect(avatarEl?.textContent?.trim()).toBe('AB');

    const badgeEl = el.querySelector('.account-badge');
    expect(badgeEl?.textContent?.trim()).toBe('ACTIVE');
  });

  it('should display all 5 KPI stats', () => {
    const el = fixture.nativeElement as HTMLElement;
    const statCards = el.querySelectorAll('.stat-card');
    expect(statCards.length).toBe(5);

    const statNumbers = Array.from(el.querySelectorAll('.stat-number')).map((n) =>
      n.textContent?.trim()
    );
    expect(statNumbers).toContain('12'); // Total Submitted
    expect(statNumbers).toContain('10'); // Approved
    expect(statNumbers).toContain('2'); // Sent Back for Revision
    expect(statNumbers).toContain('480.5'); // Hours Logged
    expect(statNumbers).toContain('35'); // Tasks Completed
  });

  it('should render report history and enforce draft privacy on DRAFT row', () => {
    const el = fixture.nativeElement as HTMLElement;
    const rows = el.querySelectorAll('tr.mat-mdc-row');
    expect(rows.length).toBe(2);

    // Row 0: APPROVED
    const row0Btn = rows[0].querySelector('button.open-btn') as HTMLButtonElement;
    expect(row0Btn.disabled).toBeFalse();

    // Row 1: DRAFT
    const row1Btn = rows[1].querySelector('button.open-btn') as HTMLButtonElement;
    expect(row1Btn.disabled).toBeTrue();
  });

  it('should navigate to review page when clicking View/Review on non-draft report', () => {
    component.openReport(mockReports[0]);
    expect(router.navigate).toHaveBeenCalledWith(['/manager/reports', 101, 'review']);
  });

  it('should not navigate when openReport is called on a draft report', () => {
    component.openReport(mockReports[1]);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should compute initials correctly for multi-word and single-word names', () => {
    expect(component.getInitials('John Doe')).toBe('JD');
    expect(component.getInitials('John')).toBe('JO');
    expect(component.getInitials('')).toBe('U');
  });
});
