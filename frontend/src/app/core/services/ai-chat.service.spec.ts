import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiChatService } from './ai-chat.service';
import { environment } from '../../../environments/environment';
import {
  AiAvailability,
  ChatMessage,
  ChatResponse,
  SummaryResponse,
} from '../models/ai-chat.model';

describe('AiChatService', () => {
  let service: AiChatService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiBaseUrl}/manager/ai-chat`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AiChatService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AiChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and default aiAvailable to false', () => {
    expect(service).toBeTruthy();
    expect(service.aiAvailable()).toBe(false);
  });

  describe('checkAvailability', () => {
    it('should query availability and update aiAvailable signal to true', () => {
      const mockResponse: AiAvailability = { enabled: true };

      service.checkAvailability().subscribe((res) => {
        expect(res.enabled).toBe(true);
        expect(service.aiAvailable()).toBe(true);
      });

      const req = httpMock.expectOne(`${baseUrl}/availability`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should query availability and update aiAvailable signal to false', () => {
      const mockResponse: AiAvailability = { enabled: false };

      service.checkAvailability().subscribe((res) => {
        expect(res.enabled).toBe(false);
        expect(service.aiAvailable()).toBe(false);
      });

      const req = httpMock.expectOne(`${baseUrl}/availability`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should reuse cached availability without making duplicate HTTP requests', () => {
      // First check
      service.checkAvailability().subscribe();
      const req1 = httpMock.expectOne(`${baseUrl}/availability`);
      req1.flush({ enabled: true });
      expect(service.aiAvailable()).toBe(true);

      // Second check should use cache
      service.checkAvailability().subscribe((res) => {
        expect(res.enabled).toBe(true);
      });
      httpMock.expectNone(`${baseUrl}/availability`);

      // Forced check should make another request
      service.checkAvailability(true).subscribe((res) => {
        expect(res.enabled).toBe(false);
      });
      const req2 = httpMock.expectOne(`${baseUrl}/availability`);
      req2.flush({ enabled: false });
      expect(service.aiAvailable()).toBe(false);
    });

    it('should handle error when checking availability gracefully', () => {
      service.checkAvailability().subscribe((res) => {
        expect(res.enabled).toBe(false);
        expect(service.aiAvailable()).toBe(false);
      });

      const req = httpMock.expectOne(`${baseUrl}/availability`);
      req.flush('Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('sendMessage', () => {
    it('should post chat message and return reply', () => {
      const history: ChatMessage[] = [
        { role: 'user', content: 'What is the status?' },
        { role: 'assistant', content: 'All reports are submitted.' },
      ];
      const message = 'Who had blockers?';
      const mockResponse: ChatResponse = { reply: 'Alice had a blocker with AWS credentials.' };

      service.sendMessage(history, message).subscribe((res) => {
        expect(res.reply).toBe(mockResponse.reply);
      });

      const req = httpMock.expectOne(`${baseUrl}/message`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ history, message });
      req.flush(mockResponse);
    });

    it('should propagate 503 Service Unavailable error when AI is disabled', () => {
      const history: ChatMessage[] = [];
      const message = 'Hello';

      service.sendMessage(history, message).subscribe({
        next: () => fail('Should have failed with 503'),
        error: (err) => {
          expect(err.status).toBe(503);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/message`);
      req.flush({ message: 'AI assistant is not configured' }, { status: 503, statusText: 'Service Unavailable' });
    });

    it('should propagate 502 Bad Gateway error when upstream Anthropic fails', () => {
      const history: ChatMessage[] = [];
      const message = 'Hello';

      service.sendMessage(history, message).subscribe({
        next: () => fail('Should have failed with 502'),
        error: (err) => {
          expect(err.status).toBe(502);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/message`);
      req.flush(
        { message: 'AI assistant is temporarily unavailable — please try again.' },
        { status: 502, statusText: 'Bad Gateway' },
      );
    });
  });

  describe('generateSummary', () => {
    it('should post summary request with weekStartDate and projectId and return summary', () => {
      const mockResponse: SummaryResponse = {
        summary: 'Team completed 15 tasks this week. No major blockers.',
      };

      service.generateSummary('2026-09-07', 42).subscribe((res) => {
        expect(res.summary).toBe(mockResponse.summary);
      });

      const req = httpMock.expectOne(`${baseUrl}/summary`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ weekStartDate: '2026-09-07', projectId: 42 });
      req.flush(mockResponse);
    });

    it('should post summary request without projectId', () => {
      const mockResponse: SummaryResponse = {
        summary: 'Team summary across all projects.',
      };

      service.generateSummary('2026-09-07').subscribe((res) => {
        expect(res.summary).toBe(mockResponse.summary);
      });

      const req = httpMock.expectOne(`${baseUrl}/summary`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ weekStartDate: '2026-09-07', projectId: undefined });
      req.flush(mockResponse);
    });

    it('should propagate 503 Service Unavailable when generating summary while disabled', () => {
      service.generateSummary('2026-09-07').subscribe({
        next: () => fail('Should have failed with 503'),
        error: (err) => {
          expect(err.status).toBe(503);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/summary`);
      req.flush({ message: 'AI assistant is not configured' }, { status: 503, statusText: 'Service Unavailable' });
    });

    it('should propagate 502 Bad Gateway when generating summary encounters upstream error', () => {
      service.generateSummary('2026-09-07').subscribe({
        next: () => fail('Should have failed with 502'),
        error: (err) => {
          expect(err.status).toBe(502);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/summary`);
      req.flush(
        { message: 'AI assistant is temporarily unavailable — please try again.' },
        { status: 502, statusText: 'Bad Gateway' },
      );
    });
  });
});

