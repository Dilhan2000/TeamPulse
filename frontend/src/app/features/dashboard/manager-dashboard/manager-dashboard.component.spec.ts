import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ManagerDashboardComponent } from './manager-dashboard.component';
import { DashboardMetricsService } from '../../../core/services/dashboard-metrics.service';
import { TeamDashboardService } from '../../../core/services/team-dashboard.service';
import { AiChatService } from '../../../core/services/ai-chat.service';
import {
  ActivityFeedItem,
  DashboardSummary,
  StatusByMember,
  TimeByTaskType,
  TrendPoint,
  WorkloadByProject,
} from '../../../core/models/dashboard.model';
import { TeamMemberOption } from '../../../core/models/report.model';

describe('ManagerDashboardComponent', () => {
  let component: ManagerDashboardComponent;
  let fixture: ComponentFixture<ManagerDashboardComponent>;
  let metricsServiceSpy: jasmine.SpyObj<DashboardMetricsService>;
  let teamDashboardServiceSpy: jasmine.SpyObj<TeamDashboardService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const mockSummary: DashboardSummary = {
    submittedThisWeek: 4,
    compliance: { submitted: 4, pending: 1, late: 0, totalActiveMembers: 5 },
    needsCorrectionCount: 2,
    openBlockersCount: 1,
    currentWeekStartDate: '2026-09-07',
  };

  const mockTrend: TrendPoint[] = [
    { weekStartDate: '2026-08-31', completedCount: 5 },
    { weekStartDate: '2026-09-07', completedCount: 8 },
  ];

  const mockStatus: StatusByMember[] = [
    { userId: 1, fullName: 'Alice Member', draft: 1, submitted: 1, needsCorrection: 0, approved: 2 },
  ];

  const mockWorkload: WorkloadByProject[] = [
    { projectId: 1, projectName: 'Project Apollo', totalHours: 40 },
  ];

  const mockTime: TimeByTaskType[] = [
    { taskType: 'DEVELOPMENT', totalHours: 25 },
    { taskType: 'TESTING', totalHours: 10 },
    { taskType: 'MEETINGS', totalHours: 5 },
    { taskType: 'DOCUMENTATION', totalHours: 2 },
    { taskType: 'OTHER', totalHours: 1 },
  ];

  const mockFeed: ActivityFeedItem[] = [
    {
      id: 'SUBMISSION-1',
      type: 'SUBMISSION',
      actorName: 'Alice Member',
      reportId: 101,
      projectName: 'Mobile App',
      weekStartDate: '2026-09-07',
      action: 'SUBMITTED',
      timestamp: '2026-09-08T10:00:00Z',
    },
  ];

  const mockTeamMembers: TeamMemberOption[] = [
    { id: 1, fullName: 'Alice Member' },
  ];

  let aiChatServiceSpy: jasmine.SpyObj<AiChatService>;
  let dialog: MatDialog;

  beforeEach(async () => {
    metricsServiceSpy = jasmine.createSpyObj('DashboardMetricsService', [
      'getSummary',
      'getTasksCompletedTrend',
      'getStatusByMember',
      'getWorkloadByProject',
      'getTimeByTaskType',
      'getActivityFeed',
    ]);
    teamDashboardServiceSpy = jasmine.createSpyObj('TeamDashboardService', ['listTeamMembers']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    aiChatServiceSpy = jasmine.createSpyObj('AiChatService', ['checkAvailability', 'generateSummary'], {
      aiAvailable: signal<boolean>(true),
    });

    metricsServiceSpy.getSummary.and.returnValue(of(mockSummary));
    metricsServiceSpy.getTasksCompletedTrend.and.returnValue(of(mockTrend));
    metricsServiceSpy.getStatusByMember.and.returnValue(of(mockStatus));
    metricsServiceSpy.getWorkloadByProject.and.returnValue(of(mockWorkload));
    metricsServiceSpy.getTimeByTaskType.and.returnValue(of(mockTime));
    metricsServiceSpy.getActivityFeed.and.returnValue(of(mockFeed));
    teamDashboardServiceSpy.listTeamMembers.and.returnValue(of(mockTeamMembers));

    await TestBed.configureTestingModule({
      imports: [ManagerDashboardComponent],
      providers: [
        provideNoopAnimations(),
        provideCharts(withDefaultRegisterables()),
        { provide: DashboardMetricsService, useValue: metricsServiceSpy },
        { provide: TeamDashboardService, useValue: teamDashboardServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: AiChatService, useValue: aiChatServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ManagerDashboardComponent);
    component = fixture.componentInstance;
    dialog = fixture.debugElement.injector.get(MatDialog);
    fixture.detectChanges();
  });

  it('should create and load all initial data', () => {
    expect(component).toBeTruthy();
    expect(metricsServiceSpy.getSummary).toHaveBeenCalled();
    expect(metricsServiceSpy.getTasksCompletedTrend).toHaveBeenCalledWith(8, undefined);
    expect(metricsServiceSpy.getStatusByMember).toHaveBeenCalled();
    expect(metricsServiceSpy.getWorkloadByProject).toHaveBeenCalled();
    expect(metricsServiceSpy.getTimeByTaskType).toHaveBeenCalled();
    expect(metricsServiceSpy.getActivityFeed).toHaveBeenCalled();
    expect(teamDashboardServiceSpy.listTeamMembers).toHaveBeenCalled();
  });

  it('should render KPI values correctly', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('4'); // submitted this week
    expect(compiled.textContent).toContain('2'); // needs correction
    expect(compiled.textContent).toContain('1'); // open blockers
  });

  it('should toggle trend scope to INDIVIDUAL and select first member', () => {
    component.onTrendScopeChange('INDIVIDUAL');
    expect(component.trendScope).toBe('INDIVIDUAL');
    expect(component.selectedUserId).toBe(1);
    expect(metricsServiceSpy.getTasksCompletedTrend).toHaveBeenCalledWith(8, 1);
  });

  it('should reload charts when weeks change', () => {
    component.selectedWeeks = 12;
    component.onWeeksChange();
    expect(metricsServiceSpy.getTasksCompletedTrend).toHaveBeenCalledWith(12, undefined);
    expect(metricsServiceSpy.getStatusByMember).toHaveBeenCalled();
    expect(metricsServiceSpy.getWorkloadByProject).toHaveBeenCalled();
    expect(metricsServiceSpy.getTimeByTaskType).toHaveBeenCalled();
  });

  it('should navigate to review queue', () => {
    component.navigateToReviewQueue();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/manager/team-reports']);
  });

  describe('AI Team Summary Quick Action (C8-T12, C8-T13)', () => {
    it('should render Generate Team Summary button when aiAvailable is true', () => {
      const summaryBtn = fixture.nativeElement.querySelector('.ai-summary-btn');
      expect(summaryBtn).toBeTruthy();
      expect(summaryBtn.textContent).toContain('Generate Team Summary');
    });

    it('should NOT render Generate Team Summary button when aiAvailable is false', () => {
      (aiChatServiceSpy.aiAvailable as any).set(false);
      fixture.detectChanges();

      const summaryBtn = fixture.nativeElement.querySelector('.ai-summary-btn');
      expect(summaryBtn).toBeFalsy();
    });

    it('should open SummaryDialogComponent scoped to the selected week when clicked', () => {
      const spy = spyOn(dialog, 'open').and.returnValue({
        afterClosed: () => of(null),
      } as any);

      component.openSummaryDialog();

      expect(spy).toHaveBeenCalledWith(
        jasmine.any(Function),
        jasmine.objectContaining({
          data: {
            weekStartDate: '2026-09-07',
          },
        }),
      );
    });
  });
});
