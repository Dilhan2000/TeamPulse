import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { TeamReportsComponent } from './team-reports.component';
import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { ManagerReportService } from '../../../core/services/manager-report.service';
import { ProjectService } from '../../../core/services/project.service';
import {
  Project,
  SectionRow,
  TeamMemberOption,
  TeamWeekStatusRow,
} from '../../../core/models/report.model';

describe('TeamReportsComponent', () => {
  let component: TeamReportsComponent;
  let fixture: ComponentFixture<TeamReportsComponent>;
  let teamDashboardSpy: jasmine.SpyObj<TeamDashboardService>;
  let managerReportSpy: jasmine.SpyObj<ManagerReportService>;
  let projectServiceSpy: jasmine.SpyObj<ProjectService>;

  const mockProjects: Project[] = [
    { id: 1, name: 'Project Alpha', description: 'Alpha Desc', active: true },
    { id: 2, name: 'Project Beta', description: 'Beta Desc', active: true },
  ];

  const mockMembers: TeamMemberOption[] = [
    { id: 10, fullName: 'Alice Member' },
    { id: 20, fullName: 'Bob Member' },
  ];

  const mockWeekRows: TeamWeekStatusRow[] = [
    {
      userId: 10,
      fullName: 'Alice Member',
      status: 'SUBMITTED',
      reportId: 101,
      projectId: 1,
      projectName: 'Project Alpha',
      submittedAt: '2026-09-08T14:30:00',
    },
    {
      userId: 20,
      fullName: 'Bob Member',
      status: 'DRAFT',
      reportId: 102,
      projectId: 2,
      projectName: 'Project Beta',
      submittedAt: null,
    },
    {
      userId: 30,
      fullName: 'Charlie Member',
      status: 'NOT_STARTED',
      reportId: null,
      projectId: null,
      projectName: null,
      submittedAt: null,
    },
  ];

  const mockSectionRows: SectionRow[] = [
    {
      reportId: 101,
      userId: 10,
      fullName: 'Alice Member',
      projectName: 'Project Alpha',
      reportStatus: 'SUBMITTED',
      items: [
        {
          description: 'Waiting for deployment credentials',
          flagged: true,
          resolved: false,
        },
      ],
    },
  ];

  beforeEach(async () => {
    teamDashboardSpy = jasmine.createSpyObj('TeamDashboardService', [
      'getTeamStatus',
      'getSection',
      'listTeamMembers',
    ]);
    managerReportSpy = jasmine.createSpyObj('ManagerReportService', ['list']);
    projectServiceSpy = jasmine.createSpyObj('ProjectService', ['listActive']);

    projectServiceSpy.listActive.and.returnValue(of(mockProjects));
    teamDashboardSpy.listTeamMembers.and.returnValue(of(mockMembers));
    teamDashboardSpy.getTeamStatus.and.returnValue(of(mockWeekRows));
    teamDashboardSpy.getSection.and.returnValue(of(mockSectionRows));
    managerReportSpy.list.and.returnValue(
      of({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 10,
        number: 0,
      })
    );

    await TestBed.configureTestingModule({
      imports: [TeamReportsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: TeamDashboardService, useValue: teamDashboardSpy },
        { provide: ManagerReportService, useValue: managerReportSpy },
        { provide: ProjectService, useValue: projectServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {},
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TeamReportsComponent);
    component = fixture.componentInstance;
  });

  it('should create component and load active projects and team members on init', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(projectServiceSpy.listActive).toHaveBeenCalled();
    expect(teamDashboardSpy.listTeamMembers).toHaveBeenCalled();
    expect(component.activeProjects().length).toBe(2);
    expect(component.teamMembers().length).toBe(2);
    expect(teamDashboardSpy.getTeamStatus).toHaveBeenCalled();
    expect(component.weekRows().length).toBe(3);
  });

  it('should enforce button disable rule for DRAFT and NOT_STARTED in Week View', () => {
    fixture.detectChanges();

    expect(component.canOpenReport('SUBMITTED')).toBeTrue();
    expect(component.canOpenReport('NEEDS_CORRECTION')).toBeTrue();
    expect(component.canOpenReport('APPROVED')).toBeTrue();
    expect(component.canOpenReport('DRAFT')).toBeFalse();
    expect(component.canOpenReport('NOT_STARTED')).toBeFalse();

    const compiled = fixture.nativeElement as HTMLElement;
    const openLinks = compiled.querySelectorAll('.action-col .open-btn');
    const disabledButtons = compiled.querySelectorAll('.action-col .disabled-btn');

    // First row is SUBMITTED (renders active Review link)
    expect(openLinks.length).toBe(1);
    // Second is DRAFT, third is NOT_STARTED (both render disabled buttons)
    expect(disabledButtons.length).toBe(2);
    expect((disabledButtons[0] as HTMLButtonElement).disabled).toBeTrue();
    expect((disabledButtons[1] as HTMLButtonElement).disabled).toBeTrue();
  });

  it('should filter week rows to SUBMITTED when Needs My Review is toggled', () => {
    fixture.detectChanges();

    expect(component.filteredWeekRows().length).toBe(3);

    component.toggleNeedsMyReview();
    expect(component.needsMyReview()).toBeTrue();
    expect(component.filteredWeekRows().length).toBe(1);
    expect(component.filteredWeekRows()[0].fullName).toBe('Alice Member');
    expect(component.filteredWeekRows()[0].status).toBe('SUBMITTED');
  });

  it('should switch to Range View tab and fetch reports via ManagerReportService.list', () => {
    fixture.detectChanges();

    component.onTabChange(1);
    expect(component.activeTab()).toBe(1);
    expect(managerReportSpy.list).toHaveBeenCalled();
  });

  it('should switch to Side-by-Side Section View and allow toggling between BLOCKERS and ACHIEVEMENTS', () => {
    fixture.detectChanges();

    component.onTabChange(2);
    expect(component.activeTab()).toBe(2);
    expect(teamDashboardSpy.getSection).toHaveBeenCalledWith(
      jasmine.any(String),
      'BLOCKERS',
      jasmine.any(Object)
    );

    component.onSectionChange('ACHIEVEMENTS');
    expect(component.selectedSection()).toBe('ACHIEVEMENTS');
    expect(teamDashboardSpy.getSection).toHaveBeenCalledWith(
      jasmine.any(String),
      'ACHIEVEMENTS',
      jasmine.any(Object)
    );
  });

  it('resetFilters should clear member, project, review toggle and reload tab', () => {
    fixture.detectChanges();

    component.selectedUserId = 10;
    component.selectedProjectId = 1;
    component.needsMyReview.set(true);

    component.resetFilters();

    expect(component.selectedUserId).toBeNull();
    expect(component.selectedProjectId).toBeNull();
    expect(component.needsMyReview()).toBeFalse();
    expect(teamDashboardSpy.getTeamStatus).toHaveBeenCalled();
  });
});
