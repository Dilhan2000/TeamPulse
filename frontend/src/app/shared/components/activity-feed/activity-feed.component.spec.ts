import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ActivityFeedComponent } from './activity-feed.component';
import { ActivityFeedItem } from '../../../core/models/dashboard.model';

describe('ActivityFeedComponent', () => {
  let component: ActivityFeedComponent;
  let fixture: ComponentFixture<ActivityFeedComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [ActivityFeedComponent],
      providers: [{ provide: Router, useValue: routerSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityFeedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display empty state when items are empty', () => {
    component.items = [];
    component.loading = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-feed')).toBeTruthy();
    expect(compiled.querySelector('.empty-title')?.textContent).toContain('No recent activity');
  });

  it('should render items when provided', () => {
    const mockItems: ActivityFeedItem[] = [
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
      {
        id: 'REVIEW-2',
        type: 'REVIEW',
        actorName: 'Bob Manager',
        targetUserName: 'Alice Member',
        reportId: 102,
        projectName: 'Backend API',
        weekStartDate: '2026-09-07',
        action: 'APPROVED',
        timestamp: '2026-09-08T11:00:00Z',
      },
    ];

    component.items = mockItems;
    component.loading = false;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const timelineItems = compiled.querySelectorAll('.timeline-item');
    expect(timelineItems.length).toBe(2);
    expect(compiled.textContent).toContain('Alice Member submitted their Week of 2026-09-07 report');
    expect(compiled.textContent).toContain("Bob Manager approved Alice Member's Week of 2026-09-07 report");
  });

  it('should navigate on item click', () => {
    const item: ActivityFeedItem = {
      id: 'SUBMISSION-1',
      type: 'SUBMISSION',
      actorName: 'Alice Member',
      reportId: 101,
      projectName: 'Mobile App',
      weekStartDate: '2026-09-07',
      action: 'SUBMITTED',
      timestamp: '2026-09-08T10:00:00Z',
    };

    component.onItemClick(item);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/manager/reports', 101, 'review']);
  });
});
