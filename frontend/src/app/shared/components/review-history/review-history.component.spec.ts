import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewHistoryComponent } from './review-history.component';
import { ReportReview } from '../../../core/models/report.model';

describe('ReviewHistoryComponent', () => {
  let component: ReviewHistoryComponent;
  let fixture: ComponentFixture<ReviewHistoryComponent>;

  const mockReviews: ReportReview[] = [
    {
      id: 1,
      reportId: 10,
      reviewerId: 2,
      reviewerName: 'Bob Manager',
      action: 'CHANGES_REQUESTED',
      comment: 'Please update testing hours.',
      reviewedAt: '2026-09-08T10:00:00',
      versionId: 1,
      versionNumber: 1,
    },
    {
      id: 2,
      reportId: 10,
      reviewerId: 2,
      reviewerName: 'Bob Manager',
      action: 'APPROVED',
      comment: 'Looks complete now, thanks!',
      reviewedAt: '2026-09-08T14:00:00',
      versionId: 2,
      versionNumber: 2,
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReviewHistoryComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReviewHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render anything when reviews array is empty', () => {
    component.reviews = [];
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.review-history-card')).toBeFalsy();
  });

  it('should render reviews timeline with actions, version badges, and comments', () => {
    component.reviews = mockReviews;
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.review-history-card')).toBeTruthy();
    expect(el.textContent).toContain('Changes Requested');
    expect(el.textContent).toContain('Approved');
    expect(el.textContent).toContain('Bob Manager');
    expect(el.textContent).toContain('Please update testing hours.');
    expect(el.textContent).toContain('Version 1');
    expect(el.textContent).toContain('Version 2');
  });
});
