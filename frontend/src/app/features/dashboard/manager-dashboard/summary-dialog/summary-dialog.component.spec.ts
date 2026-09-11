import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';

import { SummaryDialogComponent, SummaryDialogData } from './summary-dialog.component';
import { AiChatService } from '../../../../core/services/ai-chat.service';

describe('SummaryDialogComponent', () => {
  let component: SummaryDialogComponent;
  let fixture: ComponentFixture<SummaryDialogComponent>;
  let aiChatServiceSpy: jasmine.SpyObj<AiChatService>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<SummaryDialogComponent>>;

  const mockDialogData: SummaryDialogData = {
    weekStartDate: '2026-09-07',
    projectId: 10,
  };

  beforeEach(async () => {
    aiChatServiceSpy = jasmine.createSpyObj('AiChatService', ['generateSummary']);
    aiChatServiceSpy.generateSummary.and.returnValue(
      of({ summary: 'This is the generated weekly summary.' }),
    );

    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [SummaryDialogComponent],
      providers: [
        { provide: AiChatService, useValue: aiChatServiceSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SummaryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load summary on init', () => {
    expect(component).toBeTruthy();
    expect(aiChatServiceSpy.generateSummary).toHaveBeenCalledWith('2026-09-07', 10);
    expect(component.summary()).toBe('This is the generated weekly summary.');
    expect(component.loading()).toBe(false);
  });

  it('should handle 503 error on summary generation', () => {
    aiChatServiceSpy.generateSummary.and.returnValue(
      throwError(() => ({ status: 503, error: { message: 'AI assistant is not configured' } })),
    );

    component.generateSummary();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe('AI assistant is not configured.');
  });

  it('should handle 502 error on summary generation', () => {
    aiChatServiceSpy.generateSummary.and.returnValue(
      throwError(() => ({
        status: 502,
        error: { message: 'Bad Gateway' },
      })),
    );

    component.generateSummary();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBe(
      'AI assistant is temporarily unavailable — please try again.',
    );
  });

  it('should copy summary to clipboard', fakeAsync(() => {
    spyOn(navigator.clipboard, 'writeText').and.returnValue(Promise.resolve());

    component.copySummary();
    tick();

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
      'This is the generated weekly summary.',
    );
    expect(component.copied()).toBe(true);

    tick(2500);
    expect(component.copied()).toBe(false);
  }));

  it('should close dialog when close() is invoked', () => {
    component.close();
    expect(dialogRefSpy.close).toHaveBeenCalled();
  });
});
