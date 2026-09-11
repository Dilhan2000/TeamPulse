import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ReportFormComponent } from './report-form.component';
import { ProjectService } from '../../../core/services/project.service';
import { ReportService } from '../../../core/services/report.service';

describe('ReportFormComponent', () => {
  let component: ReportFormComponent;
  let fixture: ComponentFixture<ReportFormComponent>;
  let projectServiceSpy: jasmine.SpyObj<ProjectService>;
  let reportServiceSpy: jasmine.SpyObj<ReportService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    projectServiceSpy = jasmine.createSpyObj('ProjectService', ['listActive']);
    reportServiceSpy = jasmine.createSpyObj('ReportService', ['createDraft', 'updateDraft', 'submit', 'getById']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    projectServiceSpy.listActive.and.returnValue(
      of([
        { id: 1, name: 'Client A', active: true },
        { id: 2, name: 'Internal Tooling', active: true },
      ])
    );

    await TestBed.configureTestingModule({
      imports: [ReportFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        { provide: ProjectService, useValue: projectServiceSpy },
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

    fixture = TestBed.createComponent(ReportFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the form component with default empty arrays', () => {
    expect(component).toBeTruthy();
    expect(component.tasksCompletedArray.length).toBe(0);
    expect(component.tasksPlannedNextWeekArray.length).toBe(0);
    expect(component.blockersArray.length).toBe(0);
    expect(component.achievementsArray.length).toBe(0);
  });

  it('mondayDateFilter should allow Mondays and reject other days', () => {
    // 2026-09-07 is Monday
    const monday = new Date(2026, 8, 7);
    // 2026-09-08 is Tuesday
    const tuesday = new Date(2026, 8, 8);
    // 2026-09-06 is Sunday
    const sunday = new Date(2026, 8, 6);

    expect(component.mondayDateFilter(monday)).toBeTrue();
    expect(component.mondayDateFilter(tuesday)).toBeFalse();
    expect(component.mondayDateFilter(sunday)).toBeFalse();
  });

  it('should add and remove task items', () => {
    component.addTaskItem({ taskName: 'Task 1', plannedPercent: 50 });
    expect(component.tasksCompletedArray.length).toBe(1);
    expect(component.tasksCompletedArray.at(0).get('taskName')?.value).toBe('Task 1');

    component.addTaskItem({ taskName: 'Task 2' });
    expect(component.tasksCompletedArray.length).toBe(2);

    component.removeTaskItem(0);
    expect(component.tasksCompletedArray.length).toBe(1);
    expect(component.tasksCompletedArray.at(0).get('taskName')?.value).toBe('Task 2');
  });

  it('should enforce mutual exclusion on Key Issue in blockers', () => {
    component.addBlocker('Blocker 1', false);
    component.addBlocker('Blocker 2', false);

    // Set first blocker as key issue
    component.blockersArray.at(0).get('isKeyIssue')?.setValue(true);
    component.onKeyIssueToggle(0, true);

    expect(component.blockersArray.at(0).get('isKeyIssue')?.value).toBeTrue();
    expect(component.blockersArray.at(1).get('isKeyIssue')?.value).toBeFalse();

    // Now set second blocker as key issue
    component.blockersArray.at(1).get('isKeyIssue')?.setValue(true);
    component.onKeyIssueToggle(1, true);

    // First blocker must now be false
    expect(component.blockersArray.at(0).get('isKeyIssue')?.value).toBeFalse();
    expect(component.blockersArray.at(1).get('isKeyIssue')?.value).toBeTrue();
  });

  it('should enforce mutual exclusion on Key Achievement in achievements', () => {
    component.addAchievement('Achievement 1', false);
    component.addAchievement('Achievement 2', false);

    // Set first achievement as key achievement
    component.achievementsArray.at(0).get('isKeyAchievement')?.setValue(true);
    component.onKeyAchievementToggle(0, true);

    expect(component.achievementsArray.at(0).get('isKeyAchievement')?.value).toBeTrue();
    expect(component.achievementsArray.at(1).get('isKeyAchievement')?.value).toBeFalse();

    // Now set second achievement as key achievement
    component.achievementsArray.at(1).get('isKeyAchievement')?.setValue(true);
    component.onKeyAchievementToggle(1, true);

    // First achievement must now be false
    expect(component.achievementsArray.at(0).get('isKeyAchievement')?.value).toBeFalse();
    expect(component.achievementsArray.at(1).get('isKeyAchievement')?.value).toBeTrue();
  });
});
