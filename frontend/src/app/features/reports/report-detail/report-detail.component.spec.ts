import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';

import { ReportDetailComponent } from './report-detail.component';
import { ReportService } from '../../../core/services/report.service';
import { Report } from '../../../core/models/report.model';

describe('ReportDetailComponent', () => {
  let component: ReportDetailComponent;
  let fixture: ComponentFixture<ReportDetailComponent>;
  let reportServiceSpy: jasmine.SpyObj<ReportService>;

  const mockReport: Report = {
    id: 10,
    userId: 2,
    userName: 'Alice Member',
    projectId: 1,
    projectName: 'Client A',
    weekStartDate: '2026-09-07',
    weekEndDate: '2026-09-13',
    status: 'SUBMITTED',
    notes: 'Great progress this week',
    submittedAt: '2026-09-09T18:00:00',
    createdAt: '2026-09-07T09:00:00',
    tasksCompleted: [
      {
        taskName: 'Develop Auth',
        priority: 'HIGH',
        plannedPercent: 100,
        actualPercent: 100,
        status: 'COMPLETED',
        timePlannedHours: 20,
        timeSpentHours: 18,
        deliverable: 'PR #12',
        sortOrder: 0,
      },
    ],
    tasksPlannedNextWeek: [{ description: 'Develop Dashboard', sortOrder: 0 }],
    blockers: [
      { description: 'Slow CI runner', isKeyIssue: true, resolved: false, sortOrder: 0 },
    ],
    achievements: [
      { description: 'Completed SRS02', isKeyAchievement: true, sortOrder: 0 },
    ],
    hoursByType: [{ taskType: 'DEVELOPMENT', hours: 18 }],
  };

  beforeEach(async () => {
    reportServiceSpy = jasmine.createSpyObj('ReportService', ['getById']);

    await TestBed.configureTestingModule({
      imports: [ReportDetailComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ReportService, useValue: reportServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => null,
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportDetailComponent);
    component = fixture.componentInstance;
  });

  it('should render standalone presentational view when report is provided via @Input', () => {
    component.report = mockReport;
    fixture.detectChanges();

    expect(component.reportData()).toEqual(mockReport);
    expect(reportServiceSpy.getById).not.toHaveBeenCalled();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.week-heading')?.textContent).toContain('September 7, 2026');
    expect(compiled.querySelector('.project-pill')?.textContent).toContain('Client A');
    expect(compiled.querySelector('.task-name')?.textContent).toContain('Develop Auth');
    expect(compiled.querySelector('.key-issue-tag')?.textContent).toContain('KEY ISSUE');
    expect(compiled.querySelector('.key-achievement-tag')?.textContent).toContain('KEY ACHIEVEMENT');
  });

  it('computeTotalHours should sum timeSpentHours from tasks', () => {
    expect(component.computeTotalHours(mockReport)).toBe(18);
  });
});
