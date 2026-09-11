import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CorrectionBannerComponent } from './correction-banner.component';

describe('CorrectionBannerComponent', () => {
  let component: CorrectionBannerComponent;
  let fixture: ComponentFixture<CorrectionBannerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CorrectionBannerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CorrectionBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render default message when comment is not provided', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('The reviewer requested corrections');
  });

  it('should display reviewer name, date, and custom comment when provided', () => {
    component.reviewerName = 'Jane Manager';
    component.reviewedAt = '2026-09-08T14:30:00';
    component.comment = 'Please add more details to task A.';
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Jane Manager');
    expect(el.textContent).toContain('Please add more details to task A.');
  });
});
